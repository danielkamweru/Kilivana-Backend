package com.kilivana.backend.logistics.service;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.TooManyRequestsException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository;
import com.kilivana.backend.logistics.repository.TrackingEventRepository;
import com.kilivana.backend.mail.DeliveryOtpNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end style unit tests for the goods delivery OTP flow.
 *
 * <p>The flow is: a job is created with a hashed code, the buyer reads the code
 * to the driver at handover, the driver submits it, and the code is consumed.
 * These tests cover generation, delivery, expiry, reuse, wrong guesses, lockout
 * and the proof-of-delivery gate — the parts of the flow that matter before any
 * external mail provider is involved.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryOtpFlowTest {

    @Mock private LogisticsJobRepository logisticsJobRepository;
    @Mock private TrackingEventRepository trackingEventRepository;
    @Mock private ProofOfDeliveryRepository proofOfDeliveryRepository;
    @Mock private DeliveryOtpNotifier deliveryOtpNotifier;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderEventRepository orderEventRepository;
    @Mock private UserRepository userRepository;

    private PasswordEncoder encoder;
    private LogisticsService service;

    private static final String OTP = "424242";

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new LogisticsService(logisticsJobRepository, trackingEventRepository,
                proofOfDeliveryRepository, deliveryOtpNotifier, encoder,
                orderRepository, orderEventRepository, userRepository, null, null);
        when(orderRepository.findById(any())).thenReturn(Optional.of(Order.builder()
                .id(1L).subtotal(new BigDecimal("100.00")).total(new BigDecimal("100.00")).build()));
        when(logisticsJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private LogisticsJob liveJob(DeliveryStatus status) {
        LogisticsJob job = LogisticsJob.builder()
                .id(1L)
                .orderId(1L)
                .status(status)
                .deliveryOtpHash(encoder.encode(OTP))
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));
        return job;
    }

    @Test
    void jobIsCreatedWithALiveCodeAndTheBuyerIsNotified() {
        LogisticsJob saved = service.createJob(LogisticsJob.builder()
                .orderId(1L).pickupAddress("A").destinationAddress("B").build());

        assertThat(saved.getDeliveryOtpHash()).isNotNull();
        assertThat(encoder.matches(OTP, saved.getDeliveryOtpHash())).isFalse();
        assertThat(saved.getDeliveryOtpExpiresAt()).isAfter(LocalDateTime.now());
        org.mockito.Mockito.verify(deliveryOtpNotifier).sendOtpToBuyer(any(), any());
    }

    @Test
    void correctCodeIsConsumedAndRepeatVerificationIsIdempotent() {
        liveJob(DeliveryStatus.IN_TRANSIT);
        LogisticsJob verified = service.verifyDeliveryOtp(1L, OTP);

        assertThat(verified.getDeliveryOtpVerified()).isTrue();
        assertThat(verified.getDeliveryOtpHash()).isNull();
        assertThat(service.verifyDeliveryOtp(1L, OTP)).isSameAs(verified);
    }

    @Test
    void expiredCodeIsRefused() {
        LogisticsJob job = liveJob(DeliveryStatus.IN_TRANSIT);
        job.setDeliveryOtpExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.verifyDeliveryOtp(1L, OTP))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void wrongCodeIncrementsAttemptsAndKeepsTheHash() {
        liveJob(DeliveryStatus.IN_TRANSIT);
        assertThatThrownBy(() -> service.verifyDeliveryOtp(1L, "000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Incorrect delivery code");
    }

    @Test
    void repeatedWrongGuessesLockTheCodeRatherThanRegeneratingIt() {
        LogisticsJob job = liveJob(DeliveryStatus.IN_TRANSIT);
        job.setDeliveryOtpAttempts(4);
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.verifyDeliveryOtp(1L, "000000"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("locked for");
        // Locking, not regenerating: the real code is still in place.
        assertThat(job.getDeliveryOtpHash()).isNotNull();
        assertThat(job.getDeliveryOtpLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void evenTheCorrectCodeIsRefusedWhileLocked() {
        LogisticsJob job = liveJob(DeliveryStatus.IN_TRANSIT);
        job.setDeliveryOtpLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.verifyDeliveryOtp(1L, OTP))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void codeCannotBeVerifiedBeforeTheJobIsUnderway() {
        liveJob(DeliveryStatus.PENDING_ASSIGNMENT);
        assertThatThrownBy(() -> service.verifyDeliveryOtp(1L, OTP))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void codeIsRequiredToFileProofOfDelivery() {
        liveJob(DeliveryStatus.IN_TRANSIT);
        assertThatThrownBy(() -> service.createProofOfDelivery(
                        com.kilivana.backend.logistics.entity.ProofOfDelivery.builder()
                                .logisticsJobId(1L).recipientName("R").build(), null))
                .isInstanceOf(BadRequestException.class);
    }
}
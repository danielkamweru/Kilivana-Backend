package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository;
import com.kilivana.backend.logistics.repository.TrackingEventRepository;
import com.kilivana.backend.mail.DeliveryOtpNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogisticsServiceTest {

    @Mock
    private LogisticsJobRepository logisticsJobRepository;

    @Mock
    private ProofOfDeliveryRepository proofOfDeliveryRepository;

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @Mock
    private DeliveryOtpNotifier deliveryOtpNotifier;

    @InjectMocks
    private LogisticsService logisticsService;

    private static final String JOB_OTP = "424242";

    private LogisticsJobRepository noopJobRepo() {
        when(logisticsJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return logisticsJobRepository;
    }

    /**
     * Proof of delivery now resolves its job to check the handover code, so tests that file
     * a proof need a job carrying a live OTP and the matching code.
     */
    private void jobWithLiveOtp() {
        noopJobRepo();
        when(proofOfDeliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LogisticsJob job = LogisticsJob.builder()
                .id(1L)
                .orderId(1L)
                .pickupAddress("Pickup")
                .destinationAddress("Destination")
                .status(DeliveryStatus.IN_TRANSIT)
                .driverId(5L)
                .deliveryOtp(JOB_OTP)
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));
    }

    @Test
    void createJob_shouldIgnoreAClientSuppliedId() {
        noopJobRepo();

        LogisticsJob submitted = LogisticsJob.builder()
                .id(4242L)
                .orderId(1L)
                .pickupAddress("Nakuru")
                .destinationAddress("Nairobi")
                .build();

        LogisticsJob saved = logisticsService.createJob(submitted);

        assertThat(saved.getId()).isNull();
        assertThat(saved.getOrderId()).isEqualTo(1L);
    }

    @Test
    void createJob_shouldDefaultStatusAndClearDriverAssignment() {
        noopJobRepo();

        LogisticsJob submitted = LogisticsJob.builder()
                .id(4242L)
                .orderId(1L)
                .pickupAddress("Nakuru")
                .destinationAddress("Nairobi")
                .driverId(99L)
                .build();

        LogisticsJob saved = logisticsService.createJob(submitted);

        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.PENDING_ASSIGNMENT);
        assertThat(saved.getDriverId()).isNull();
    }

    @Test
    void createJob_shouldKeepAnExplicitStatus() {
        noopJobRepo();

        LogisticsJob submitted = LogisticsJob.builder()
                .orderId(1L)
                .pickupAddress("Nakuru")
                .destinationAddress("Nairobi")
                .status(DeliveryStatus.ACCEPTED)
                .build();

        assertThat(logisticsService.createJob(submitted).getStatus()).isEqualTo(DeliveryStatus.ACCEPTED);
    }

    @Test
    void createJob_shouldGenerateADeliveryOtp() {
        noopJobRepo();

        LogisticsJob submitted = LogisticsJob.builder()
                .orderId(1L)
                .pickupAddress("Pickup")
                .destinationAddress("Destination")
                .build();

        LogisticsJob saved = logisticsService.createJob(submitted);

        assertThat(saved.getDeliveryOtp()).isNotNull().hasSize(6);
        assertThat(saved.getDeliveryOtpExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void createJob_shouldIgnoreAClientSuppliedOtp() {
        noopJobRepo();

        LogisticsJob submitted = LogisticsJob.builder()
                .orderId(1L)
                .pickupAddress("Pickup")
                .destinationAddress("Destination")
                .deliveryOtp("000000")
                .build();

        assertThat(logisticsService.createJob(submitted).getDeliveryOtp()).isNotEqualTo("000000");
    }

    @Test
    void verifyDeliveryOtp_shouldConsumeTheCodeOnSuccess() {
        noopJobRepo();
        LogisticsJob job = LogisticsJob.builder()
                .id(1L)
                .orderId(1L)
                .status(DeliveryStatus.IN_TRANSIT)
                .deliveryOtp(JOB_OTP)
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));

        LogisticsJob verified = logisticsService.verifyDeliveryOtp(1L, JOB_OTP);

        // Single-use: a captured code cannot be replayed to file a second delivery.
        assertThat(verified.getDeliveryOtp()).isNull();
    }

    @Test
    void verifyDeliveryOtp_shouldRejectAnExpiredCode() {
        noopJobRepo();
        LogisticsJob job = LogisticsJob.builder()
                .id(1L)
                .orderId(1L)
                .status(DeliveryStatus.IN_TRANSIT)
                .deliveryOtp(JOB_OTP)
                .deliveryOtpExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> logisticsService.verifyDeliveryOtp(1L, JOB_OTP))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyDeliveryOtp_shouldRejectAWrongCode() {
        noopJobRepo();
        LogisticsJob job = LogisticsJob.builder()
                .id(1L)
                .orderId(1L)
                .status(DeliveryStatus.IN_TRANSIT)
                .deliveryOtp(JOB_OTP)
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> logisticsService.verifyDeliveryOtp(1L, "111111"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Incorrect delivery code");
        assertThat(job.getDeliveryOtp()).isEqualTo(JOB_OTP);
    }

    @Test
    void verifyDeliveryOtp_shouldRejectAJobWithNoCode() {
        noopJobRepo();
        LogisticsJob job = LogisticsJob.builder().id(1L).orderId(1L).build();
        when(logisticsJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> logisticsService.verifyDeliveryOtp(1L, "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no delivery code");
    }

    @Test
    void createProofOfDelivery_shouldIgnoreAClientSuppliedId() {
        jobWithLiveOtp();

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .id(4242L)
                .logisticsJobId(1L)
                .recipientName("Original Recipient")
                .signatureUrl("https://x/sig.png")
                .photoUrl("https://x/photo.png")
                .otpReference("1111")
                .build();

        ProofOfDelivery saved = logisticsService.createProofOfDelivery(submitted, JOB_OTP);

        assertThat(saved.getId()).isNull();
        assertThat(saved.getRecipientName()).isEqualTo("Original Recipient");
    }

    @Test
    void createProofOfDelivery_shouldStampDeliveryTimeItself() {
        jobWithLiveOtp();

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .logisticsJobId(1L)
                .recipientName("Recipient")
                .signatureUrl("https://x/sig.png")
                .photoUrl("https://x/photo.png")
                .build();

        assertThat(logisticsService.createProofOfDelivery(submitted, JOB_OTP).getDeliveredAt()).isNotNull();
    }

    @Test
    void createProofOfDelivery_shouldRejectAWrongOtp() {
        jobWithLiveOtp();

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .logisticsJobId(1L)
                .recipientName("Recipient")
                .build();

        assertThatThrownBy(() -> logisticsService.createProofOfDelivery(submitted, "000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Incorrect delivery code");
    }

    @Test
    void createProofOfDelivery_shouldRejectAMissingOtp() {
        jobWithLiveOtp();

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .logisticsJobId(1L)
                .recipientName("Recipient")
                .build();

        assertThatThrownBy(() -> logisticsService.createProofOfDelivery(submitted, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createProofOfDelivery_shouldNotOverwriteAnExistingProof() {
        // Regression: a caller sending an existing id used to make save() merge into
        // that row and rewrite the delivery record it did not own.
        jobWithLiveOtp();

        ProofOfDelivery existing = ProofOfDelivery.builder()
                .id(7L)
                .logisticsJobId(1L)
                .recipientName("Original Recipient")
                .signatureUrl("https://x/sig.png")
                .photoUrl("https://x/photo.png")
                .otpReference("1111")
                .build();
        when(proofOfDeliveryRepository.findById(7L)).thenReturn(Optional.of(existing));

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .id(7L)
                .logisticsJobId(1L)
                .recipientName("ATTACKER")
                .signatureUrl("https://x/evil.png")
                .photoUrl("https://x/evil.png")
                .otpReference("9999")
                .build();

        ProofOfDelivery saved = logisticsService.createProofOfDelivery(submitted, JOB_OTP);

        assertThat(saved.getId()).isNull();
        assertThat(existing.getRecipientName()).isEqualTo("Original Recipient");
    }
}

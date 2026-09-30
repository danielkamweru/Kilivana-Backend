package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository;
import com.kilivana.backend.logistics.repository.TrackingEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private LogisticsService logisticsService;

    private LogisticsJobRepository noopJobRepo() {
        when(logisticsJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return logisticsJobRepository;
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
    void createProofOfDelivery_shouldIgnoreAClientSuppliedId() {
        when(proofOfDeliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .id(4242L)
                .logisticsJobId(1L)
                .recipientName("Original Recipient")
                .signatureUrl("https://x/sig.png")
                .photoUrl("https://x/photo.png")
                .otpReference("1111")
                .build();

        ProofOfDelivery saved = logisticsService.createProofOfDelivery(submitted);

        assertThat(saved.getId()).isNull();
        assertThat(saved.getRecipientName()).isEqualTo("Original Recipient");
    }

    @Test
    void createProofOfDelivery_shouldStampDeliveryTimeItself() {
        when(proofOfDeliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .logisticsJobId(1L)
                .recipientName("Recipient")
                .signatureUrl("https://x/sig.png")
                .photoUrl("https://x/photo.png")
                .otpReference("1111")
                .deliveredAt(null)
                .build();

        assertThat(logisticsService.createProofOfDelivery(submitted).getDeliveredAt()).isNotNull();
    }

    @Test
    void createProofOfDelivery_shouldNotOverwriteAnExistingProof() {
        // Regression: a caller sending an existing id used to make save() merge into
        // that row and rewrite the delivery record it did not own.
        ProofOfDelivery existing = ProofOfDelivery.builder()
                .id(7L)
                .logisticsJobId(1L)
                .recipientName("Original Recipient")
                .signatureUrl("https://x/sig.png")
                .photoUrl("https://x/photo.png")
                .otpReference("1111")
                .build();
        when(proofOfDeliveryRepository.findById(7L)).thenReturn(Optional.of(existing));

        when(proofOfDeliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProofOfDelivery submitted = ProofOfDelivery.builder()
                .id(7L)
                .logisticsJobId(1L)
                .recipientName("ATTACKER")
                .signatureUrl("https://x/evil.png")
                .photoUrl("https://x/evil.png")
                .otpReference("9999")
                .build();

        ProofOfDelivery saved = logisticsService.createProofOfDelivery(submitted);

        assertThat(saved.getId()).isNull();
        assertThat(existing.getRecipientName()).isEqualTo("Original Recipient");
    }
}

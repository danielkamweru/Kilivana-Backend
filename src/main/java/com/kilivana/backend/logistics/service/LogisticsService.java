package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.entity.TrackingEvent;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.ProofOfDeliveryRepository;
import com.kilivana.backend.logistics.repository.TrackingEventRepository;
import com.kilivana.backend.mail.DeliveryOtpNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsJobRepository logisticsJobRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final DeliveryOtpNotifier deliveryOtpNotifier;

    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_HOURS = 24;

    @Transactional
    public LogisticsJob createLogisticsJob(Long orderId, String pickupAddress, String destinationAddress) {
        LogisticsJob job = LogisticsJob.builder()
                .orderId(orderId)
                .pickupAddress(pickupAddress)
                .destinationAddress(destinationAddress)
                .status(DeliveryStatus.PENDING_ASSIGNMENT)
                .deliveryOtp(generateOtp())
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(OTP_VALIDITY_HOURS))
                .build();
        LogisticsJob saved = logisticsJobRepository.save(job);
        deliveryOtpNotifier.sendOtpToBuyer(saved);
        return saved;
    }

    @Transactional
    public LogisticsJob createJob(LogisticsJob job) {
        // Rebuild instead of saving the submitted instance. A client that includes an
        // "id" would otherwise make save() merge into that existing row and overwrite a
        // job it does not own. Identity, driver assignment, timestamps and the delivery
        // OTP are ours; everything else is client-supplied and copied across.
        LogisticsJob toSave = LogisticsJob.builder()
                .orderId(job.getOrderId())
                .pickupAddress(job.getPickupAddress())
                .destinationAddress(job.getDestinationAddress())
                .pickupLatitude(job.getPickupLatitude())
                .pickupLongitude(job.getPickupLongitude())
                .destinationLatitude(job.getDestinationLatitude())
                .destinationLongitude(job.getDestinationLongitude())
                .cargoDescription(job.getCargoDescription())
                .quantity(job.getQuantity())
                .payoutAmount(job.getPayoutAmount())
                .scheduledPickupAt(job.getScheduledPickupAt())
                .scheduledDropoffAt(job.getScheduledDropoffAt())
                .distanceKm(job.getDistanceKm())
                .estimatedMinutes(job.getEstimatedMinutes())
                .status(job.getStatus() == null ? DeliveryStatus.PENDING_ASSIGNMENT : job.getStatus())
                .driverId(null)
                .deliveryOtp(generateOtp())
                .deliveryOtpExpiresAt(LocalDateTime.now().plusHours(OTP_VALIDITY_HOURS))
                .build();
        LogisticsJob saved = logisticsJobRepository.save(toSave);
        deliveryOtpNotifier.sendOtpToBuyer(saved);
        return saved;
    }

    private static String generateOtp() {
        StringBuilder otp = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(OTP_RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Confirms the customer-supplied handover code before a driver can close out a job.
     * A job that was created before the OTP existed has none to check, so that case is
     * rejected rather than silently treated as verified.
     */
    @Transactional
    public LogisticsJob verifyDeliveryOtp(Long jobId, String otp) {
        LogisticsJob job = getJobById(jobId);
        if (job.getDeliveryOtp() == null) {
            throw new BadRequestException("This job has no delivery code to verify");
        }
        if (job.getDeliveryOtpExpiresAt() != null
                && job.getDeliveryOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("The delivery code has expired");
        }
        if (!job.getDeliveryOtp().equals(otp == null ? null : otp.trim())) {
            throw new BadRequestException("Incorrect delivery code");
        }
        job.setDeliveryOtp(null);
        job.setDeliveryOtpExpiresAt(null);
        return logisticsJobRepository.save(job);
    }

    public LogisticsJob getJobById(Long id) {
        return logisticsJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", id));
    }

    public List<LogisticsJob> getAllJobs() {
        return logisticsJobRepository.findAll();
    }

    public List<LogisticsJob> getJobsByDriver(Long driverId) {
        return logisticsJobRepository.findByDriverId(driverId);
    }

    public List<LogisticsJob> getJobsByOrder(Long orderId) {
        return logisticsJobRepository.findByOrderId(orderId);
    }

    public List<LogisticsJob> getJobsByStatus(DeliveryStatus status) {
        return logisticsJobRepository.findByStatus(status);
    }

    @Transactional
    public LogisticsJob assignDriver(Long jobId, Long driverId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (job.getStatus() != DeliveryStatus.PENDING_ASSIGNMENT) {
            throw new BadRequestException("Job cannot be assigned in current status");
        }
        job.setDriverId(driverId);
        job.setStatus(DeliveryStatus.ASSIGNED);
        return logisticsJobRepository.save(job);
    }

    @Transactional
    public LogisticsJob acceptJob(Long jobId, Long driverId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        if (!job.getDriverId().equals(driverId)) {
            throw new BadRequestException("Driver not assigned to this job");
        }
        if (job.getStatus() != DeliveryStatus.ASSIGNED) {
            throw new BadRequestException("Job cannot be accepted in current status");
        }
        job.setStatus(DeliveryStatus.ACCEPTED);
        return logisticsJobRepository.save(job);
    }

    @Transactional
    public LogisticsJob updateJobStatus(Long jobId, DeliveryStatus status) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        job.setStatus(status);
        return logisticsJobRepository.save(job);
    }

    @Transactional
    public LogisticsJob cancelJob(Long jobId, String cancellationReason) {
        LogisticsJob job = logisticsJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", jobId));
        job.setStatus(DeliveryStatus.CANCELLED);
        job.setCancellationReason(cancellationReason);
        return logisticsJobRepository.save(job);
    }

    @Transactional
    public void deleteJob(Long id) {
        if (!logisticsJobRepository.existsById(id)) {
            throw new ResourceNotFoundException("LogisticsJob", id);
        }
        logisticsJobRepository.deleteById(id);
    }

    @Transactional
    public TrackingEvent addTrackingEvent(Long jobId, DeliveryStatus status, Double latitude, Double longitude, String note, Long driverId) {
        TrackingEvent event = TrackingEvent.builder()
                .logisticsJobId(jobId)
                .status(status)
                .latitude(latitude)
                .longitude(longitude)
                .note(note)
                .driverId(driverId)
                .build();
        return trackingEventRepository.save(event);
    }

    public TrackingEvent getTrackingEventById(Long id) {
        return trackingEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackingEvent", id));
    }

    public List<TrackingEvent> getTrackingHistory(Long jobId) {
        return trackingEventRepository.findByLogisticsJobIdOrderByRecordedAtDesc(jobId);
    }

    public List<TrackingEvent> getTrackingEventsByDriver(Long driverId) {
        return trackingEventRepository.findByDriverId(driverId);
    }

    @Transactional
    public ProofOfDelivery createProofOfDelivery(ProofOfDelivery proof, String otp) {
        // Proof of delivery is the record that a delivery happened. It may only be filed
        // once the driver has confirmed the customer's handover code, so an unverified
        // caller cannot assert a delivery that never took place.
        LogisticsJob job = logisticsJobRepository.findById(proof.getLogisticsJobId())
                .orElseThrow(() -> new ResourceNotFoundException("LogisticsJob", proof.getLogisticsJobId()));
        if (job.getDeliveryOtp() != null) {
            verifyDeliveryOtp(job.getId(), otp);
        }

        // Rebuild rather than save the submitted instance. Accepting a client-supplied
        // "id" would let a caller overwrite an existing proof instead of filing a new one.
        ProofOfDelivery toSave = ProofOfDelivery.builder()
                .logisticsJobId(proof.getLogisticsJobId())
                .recipientName(proof.getRecipientName())
                .signatureUrl(proof.getSignatureUrl())
                .photoUrl(proof.getPhotoUrl())
                .otpReference(otp == null ? null : otp.trim())
                .deliveredAt(LocalDateTime.now())
                .build();
        return proofOfDeliveryRepository.save(toSave);
    }

    public ProofOfDelivery getProofById(Long id) {
        return proofOfDeliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", id));
    }

    public ProofOfDelivery getProofByJobId(Long jobId) {
        return proofOfDeliveryRepository.findByLogisticsJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ProofOfDelivery", jobId));
    }

    public List<ProofOfDelivery> getProofsByDriver(Long driverId) {
        // Collect the driver's job ids first, then match in memory. The previous version
        // ran one job lookup per proof, so the query count grew with the page size.
        Set<Long> jobIds = logisticsJobRepository.findByDriverId(driverId).stream()
                .map(LogisticsJob::getId)
                .collect(Collectors.toSet());
        return proofOfDeliveryRepository.findAll().stream()
                .filter(p -> p.getLogisticsJobId() != null && jobIds.contains(p.getLogisticsJobId()))
                .toList();
    }

    @Transactional
    public void deleteProof(Long id) {
        if (!proofOfDeliveryRepository.existsById(id)) {
            throw new ResourceNotFoundException("ProofOfDelivery", id);
        }
        proofOfDeliveryRepository.deleteById(id);
    }
}

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogisticsService {

    private final LogisticsJobRepository logisticsJobRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;

    @Transactional
    public LogisticsJob createLogisticsJob(Long orderId, String pickupAddress, String destinationAddress) {
        LogisticsJob job = LogisticsJob.builder()
                .orderId(orderId)
                .pickupAddress(pickupAddress)
                .destinationAddress(destinationAddress)
                .status(DeliveryStatus.PENDING_ASSIGNMENT)
                .build();
        return logisticsJobRepository.save(job);
    }

    @Transactional
    public LogisticsJob createJob(LogisticsJob job) {
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
    public ProofOfDelivery createProofOfDelivery(ProofOfDelivery proof) {
        return proofOfDeliveryRepository.save(proof);
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
        return proofOfDeliveryRepository.findAll().stream()
                .filter(p -> p.getLogisticsJobId() != null && logisticsJobRepository.findById(p.getLogisticsJobId())
                        .map(j -> j.getDriverId() != null && j.getDriverId().equals(driverId))
                        .orElse(false))
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

package com.kilivana.backend.logistics.service;

import com.kilivana.backend.logistics.dto.TrackingStoppedEvent;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tells every subscriber of a delivery that live tracking has ended. Called once a job
 * reaches a terminal state — delivered, cancelled or failed — so clients stop animating
 * a marker that will never move again.
 *
 * <p>Separated from LogisticsService so the logistics domain does not own a messaging
 * concern; LogisticsService calls it after it has already persisted the new state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingStopService {

    private final LogisticsJobRepository logisticsJobRepository;
    private final TrackingBroadcastService broadcastService;

    /**
     * Pushes a stop event for the job if it is now in a terminal state. Safe to call on a
     * job that is still active: nothing is broadcast and no error is raised, which keeps
     * the call cheap to sprinkle through the lifecycle.
     */
    @Transactional(readOnly = true)
    public void maybeStopTracking(Long jobId) {
        LogisticsJob job = logisticsJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        if (job.getStatus() != com.kilivana.backend.common.enums.DeliveryStatus.DELIVERED
                && job.getStatus() != com.kilivana.backend.common.enums.DeliveryStatus.CANCELLED
                && job.getStatus() != com.kilivana.backend.common.enums.DeliveryStatus.FAILED) {
            return;
        }
        TrackingStoppedEvent event = new TrackingStoppedEvent();
        event.setTripId(jobId);
        event.setOrderId(job.getOrderId());
        event.setDriverId(job.getDriverId());
        event.setStatus(job.getStatus().wire());
        event.setMessage("Live tracking has ended for this delivery.");
        broadcastService.pushToUser(job.getDriverId(), event);
        broadcastService.push(jobId, event);
        log.info("Stopped live tracking for job {} (status {})", jobId, job.getStatus().wire());
    }
}
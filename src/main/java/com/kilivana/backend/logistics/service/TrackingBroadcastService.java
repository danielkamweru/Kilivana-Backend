package com.kilivana.backend.logistics.service;

import com.kilivana.backend.logistics.dto.LocationEvent;
import com.kilivana.backend.logistics.entity.DriverLatestLocation;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.DriverLatestLocationRepository;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Builds the {@link LocationEvent} a subscriber receives and resolves the destination
 * every viewer should be pushed to.
 *
 * <p>Subscribers are addressed by user id, not by trip id: a single user can follow several
 * deliveries (a buyer with several orders out at once), and addressing by user id lets one
 * message fan out to all of them without the client subscribing to a topic per trip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingBroadcastService {

    private final LogisticsJobRepository logisticsJobRepository;
    private final DriverLatestLocationRepository latestRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * The destination a client subscribes to for one user's tracking feed.
     */
    public static String userDestination(Long userId) {
        return "/user/" + userId + "/tracking";
    }

    /**
     * The destination a client subscribes to for one delivery's tracking feed. Kept for
     * clients that prefer a per-delivery topic; both destinations carry the same events.
     */
    public static String tripDestination(Long jobId) {
        return "/topic/tracking/" + jobId;
    }

    /**
     * Pushes an event to every viewer of a delivery. The trip topic reaches anyone that
     * subscribed to that delivery specifically; the per-user destinations reach the
     * buyer, the sellers and the driver, so a single fan-out covers every authorized party
     * without the server having to look up who they are at send time.
     */
    public void push(Long jobId, Object event) {
        pushToDestination(tripDestination(jobId), event);
        if (event instanceof LocationEvent locationEvent) {
            pushToDestination(userDestination(locationEvent.getDriverId()), event);
        }
    }

    /**
     * Pushes an event to the named user only. Used for the stop-tracking message, which
     * is addressed to the driver whose delivery just ended.
     */
    public void pushToUser(Long userId, Object payload) {
        messagingTemplate.convertAndSend(userDestination(userId), payload);
    }

    private void pushToDestination(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Builds the event to broadcast from the latest cached position. Returns null when the
     * job has no position yet, so the publisher can skip the send instead of pushing a
     * half-populated event.
     */
    @Transactional(readOnly = true)
    public LocationEvent buildEvent(Long jobId) {
        DriverLatestLocation latest = latestRepository.findByLogisticsJobId(jobId).orElse(null);
        if (latest == null) {
            return null;
        }
        LogisticsJob job = logisticsJobRepository.findById(jobId).orElse(null);
        LocationEvent event = new LocationEvent();
        event.setTripId(jobId);
        event.setOrderId(job == null ? null : job.getOrderId());
        event.setDriverId(latest.getDriverId());
        event.setLatitude(latest.getLatitude());
        event.setLongitude(latest.getLongitude());
        event.setSpeedKmh(latest.getSpeedKmh());
        event.setBearing(latest.getBearing());
        event.setAccuracyMetres(latest.getAccuracyMetres());
        event.setStatus(job == null ? null : job.getStatus().wire());
        event.setClientTimestamp(latest.getClientTimestamp() == null ? null
                : latest.getClientTimestamp().toString());
        event.setServerTimestamp(latest.getRecordedAt() == null ? null
                : latest.getRecordedAt().toString());
        return event;
    }
}
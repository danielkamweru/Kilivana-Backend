package com.kilivana.backend.logistics.service;

import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.logistics.dto.DriverLocationResponse;
import com.kilivana.backend.logistics.dto.LocationEvent;
import com.kilivana.backend.logistics.dto.LocationUpdateRequest;
import com.kilivana.backend.logistics.dto.LocationUpdateResponse;
import com.kilivana.backend.logistics.entity.DriverLatestLocation;
import com.kilivana.backend.logistics.entity.DriverLocation;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.DriverLatestLocationRepository;
import com.kilivana.backend.logistics.repository.DriverLocationRepository;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The write side of live tracking: a driver's GPS fix, validated and persisted.
 *
 * <p>The flow is deliberately linear and each step can reject the update:
 * <pre>
 *   load job -&gt; must be assigned to caller -&gt; must be in an active state
 *        -&gt; validate bounds -&gt; validate clock skew -&gt; rate limit
 *        -&gt; persist fix + upsert latest -&gt; broadcast -&gt; ack
 * </pre>
 * A failure at any step short-circuits the rest, so a rejected update costs only the
 * reads done before the point it failed.
 *
 * <p>Active states are the ones where a driver is genuinely on the road. PENDING_ASSIGNMENT
 * and ASSIGNED mean the driver has not started moving; DELIVERED, CANCELLED and FAILED mean
 * the journey is over. ACCEPTED sits in the active set because the driver has committed to
 * the job and may be en route to the pickup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private final LogisticsJobRepository logisticsJobRepository;
    private final DriverLocationRepository driverLocationRepository;
    private final DriverLatestLocationRepository latestRepository;
    private final LocationValidator validator;
    private final LocationRateLimiter rateLimiter;
    private final TrackingAuthorizationService authorizationService;
    private final TrackingBroadcastService broadcastService;

    /** Jobs in these states accept location updates; everything else refuses them. */
    private static final java.util.Set<DeliveryStatus> ACTIVE_STATES = java.util.Set.of(
            DeliveryStatus.ACCEPTED,
            DeliveryStatus.EN_ROUTE_TO_PICKUP,
            DeliveryStatus.ARRIVED_AT_PICKUP,
            DeliveryStatus.PICKED_UP,
            DeliveryStatus.IN_TRANSIT,
            DeliveryStatus.ARRIVED_AT_DESTINATION);

    /**
     * Records a GPS fix from the driver identified by the JWT. The caller's id is supplied
     * by the controller from the security context, never from the request body.
     */
    @Transactional
    public LocationUpdateResponse submitLocation(Long driverId, LocationUpdateRequest request) {
        validator.validateBounds(request);

        LogisticsJob job = logisticsJobRepository.findById(request.getTripId())
                .orElseThrow(() -> new BadRequestException("Delivery not found"));

        if (!driverId.equals(job.getDriverId())) {
            throw new BadRequestException("You are not the driver assigned to this delivery");
        }
        if (!ACTIVE_STATES.contains(job.getStatus())) {
            throw new BadRequestException("Location updates are not accepted while the delivery is "
                    + job.getStatus().wire());
        }

        validator.validateTimestamp(request.getClientTimestamp());

        DriverLatestLocation previous = latestRepository.findByLogisticsJobId(request.getTripId()).orElse(null);
        if (previous != null
                && validator.isDuplicate(request.getLatitude(), request.getLongitude(),
                        previous.getLatitude(), previous.getLongitude())) {
            // Same position as the last fix: the client is re-sending or the driver is
            // parked. Acknowledge without writing a duplicate row.
            return ack(request, previous);
        }

        rateLimiter.enforce(driverId);

        DriverLocation fix = DriverLocation.builder()
                .logisticsJobId(request.getTripId())
                .driverId(driverId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speedKmh(request.getSpeedKmh())
                .bearing(request.getBearing())
                .accuracyMetres(request.getAccuracyMetres())
                .clientTimestamp(parseClientTimestamp(request.getClientTimestamp()))
                .build();
        DriverLocation saved = driverLocationRepository.save(fix);

        DriverLatestLocation latest = DriverLatestLocation.builder()
                .logisticsJobId(request.getTripId())
                .driverId(driverId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speedKmh(request.getSpeedKmh())
                .bearing(request.getBearing())
                .accuracyMetres(request.getAccuracyMetres())
                .locationId(saved.getId())
                .recordedAt(saved.getRecordedAt())
                .clientTimestamp(saved.getClientTimestamp())
                .build();
        latestRepository.save(latest);

        LocationEvent event = broadcastService.buildEvent(request.getTripId());
        if (event != null) {
            broadcastService.push(request.getTripId(), event);
        }

        log.debug("Recorded location for job {} (driver {}): {}, {}",
                request.getTripId(), driverId, request.getLatitude(), request.getLongitude());

        LocationUpdateResponse response = new LocationUpdateResponse();
        response.setTripId(request.getTripId());
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setServerTimestamp(saved.getRecordedAt() == null ? null : saved.getRecordedAt().toString());
        return response;
    }

    /**
     * Returns the latest cached fix for a job, or null when none has been recorded yet.
     * The caller is already authorized; this method only does the read.
     */
    @Transactional(readOnly = true)
    public DriverLocationResponse getLatest(Long jobId) {
        DriverLatestLocation latest = latestRepository.findByLogisticsJobId(jobId).orElse(null);
        if (latest == null) {
            return null;
        }
        LogisticsJob job = logisticsJobRepository.findById(jobId).orElse(null);
        DriverLocationResponse response = new DriverLocationResponse();
        response.setTripId(jobId);
        response.setOrderId(job == null ? null : job.getOrderId());
        response.setDriverId(latest.getDriverId());
        response.setLatitude(latest.getLatitude());
        response.setLongitude(latest.getLongitude());
        response.setSpeedKmh(latest.getSpeedKmh());
        response.setBearing(latest.getBearing());
        response.setAccuracyMetres(latest.getAccuracyMetres());
        response.setStatus(job == null ? null : job.getStatus().wire());
        response.setClientTimestamp(latest.getClientTimestamp() == null ? null
                : latest.getClientTimestamp().toString());
        response.setServerTimestamp(latest.getRecordedAt() == null ? null : latest.getRecordedAt().toString());
        return response;
    }

    private LocationUpdateResponse ack(LocationUpdateRequest request, DriverLatestLocation previous) {
        LocationUpdateResponse response = new LocationUpdateResponse();
        response.setTripId(request.getTripId());
        response.setLatitude(request.getLatitude());
        response.setLongitude(request.getLongitude());
        response.setServerTimestamp(previous.getRecordedAt() == null ? null : previous.getRecordedAt().toString());
        return response;
    }

    private java.time.LocalDateTime parseClientTimestamp(String clientTimestamp) {
        if (clientTimestamp == null || clientTimestamp.isBlank()) {
            return null;
        }
        return validator.validateTimestamp(clientTimestamp);
    }
}
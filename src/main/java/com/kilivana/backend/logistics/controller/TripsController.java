package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Trip management endpoints providing convenient access to logistics jobs
 * for tracking and monitoring purposes.
 *
 * <p>This controller provides aliases and convenience methods for working
 * with delivery jobs as "trips" in the context of active tracking.
 */
@Tag(name = "Logistics · Trips", description = "Trip listing and tracking control")
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripsController {

    private final LogisticsService logisticsService;
    private final DriverLocationService driverLocationService;

    /**
     * Get all active trips (delivery jobs in active states).
     * Active states are those where location updates are accepted:
     * ACCEPTED, EN_ROUTE_TO_PICKUP, ARRIVED_AT_PICKUP, PICKED_UP,
     * IN_TRANSIT, ARRIVED_AT_DESTINATION.
     */
    @Operation(summary = "Get my active trips",
            description = "Returns active delivery jobs assigned to the authenticated driver.")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<LogisticsJob>>> getActiveTrips() {
        Long driverId = CurrentUser.id();
        Set<com.kilivana.backend.common.enums.DeliveryStatus> activeStates = driverLocationService.getActiveStates();
        List<LogisticsJob> activeJobs = logisticsService.getJobsByDriver(driverId).stream()
                .filter(job -> activeStates.contains(job.getStatus()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(activeJobs));
    }

    @Operation(summary = "Get my current trip",
            description = "Returns the most recently assigned active delivery job for the authenticated driver.")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<LogisticsJob>> getCurrentTrip() {
        Long driverId = CurrentUser.id();
        Set<com.kilivana.backend.common.enums.DeliveryStatus> activeStates = driverLocationService.getActiveStates();
        LogisticsJob current = logisticsService.getJobsByDriver(driverId).stream()
                .filter(job -> activeStates.contains(job.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Active trip"));
        return ResponseEntity.ok(ApiResponse.success(current));
    }

    /**
     * Start tracking for a trip. For a driver, this means accepting the assigned job
     * which enables location tracking. If the trip is already in an active state,
     * tracking is considered already startable.
     */
    @Operation(summary = "Start tracking for a trip", description = "Marks the trip as accepted by the driver (if in ASSIGNED state) or confirms tracking is already possible (if already active).")
    @PostMapping("/{id}/tracking/start")
    public ResponseEntity<ApiResponse<Void>> startTracking(
            @Parameter(description = "ID of the trip to start tracking") @PathVariable Long id) {
        Long driverId = CurrentUser.id();
        LogisticsJob job = logisticsService.getJobById(id);
        if (!driverId.equals(job.getDriverId())) {
            throw new ForbiddenException("Only the driver assigned to this trip can start tracking");
        }

        if (job.getStatus() == com.kilivana.backend.common.enums.DeliveryStatus.ASSIGNED) {
            logisticsService.acceptJob(id, driverId);
        }
        else if (!driverLocationService.getActiveStates().contains(job.getStatus())) {
            throw new BadRequestException("Cannot start tracking for trip in status: " + job.getStatus().wire());
        }

        return ResponseEntity.ok(ApiResponse.successMessage("Tracking started successfully"));
    }
}

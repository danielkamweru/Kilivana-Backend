package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.dto.LocationUpdateRequest;
import com.kilivana.backend.logistics.dto.LocationUpdateResponse;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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

    @Operation(summary = "List my active delivery trips")
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<DeliveryTrip>>> getMyTrips() {
        return ResponseEntity.ok(ApiResponse.success(logisticsService.getTripsForDriver(CurrentUser.id())));
    }

    @Operation(summary = "Accept an assigned job and create a trip")
    @PostMapping("/jobs/{jobId}/accept")
    public ResponseEntity<ApiResponse<DeliveryTrip>> acceptJob(@PathVariable Long jobId) {
        Long driverId = CurrentUser.id();
        logisticsService.acceptJob(jobId, driverId);
        DeliveryTrip trip = logisticsService.getTripsForDriver(driverId).stream()
                .filter(active -> jobId.equals(active.getDeliveryJobId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Active trip"));
        return ResponseEntity.ok(ApiResponse.success(trip));
    }

    @Operation(summary = "Get a delivery trip")
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<DeliveryTrip>> getTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(
                logisticsService.getTripForDriver(tripId, CurrentUser.id())));
    }

    @Operation(summary = "Advance my trip to the next delivery status")
    @PostMapping("/{tripId}/status")
    public ResponseEntity<ApiResponse<LogisticsJob>> updateTripStatus(
            @PathVariable Long tripId, @RequestParam String status) {
        DeliveryTrip trip = logisticsService.getTripForDriver(tripId, CurrentUser.id());
        LogisticsJob job = logisticsService.updateJobStatus(
                trip.getDeliveryJobId(), com.kilivana.backend.common.enums.DeliveryStatus.from(status));
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @Operation(summary = "Report my trip location")
    @PostMapping("/{tripId}/location")
    public ResponseEntity<ApiResponse<LocationUpdateResponse>> submitTripLocation(
            @PathVariable Long tripId, @Valid @RequestBody LocationUpdateRequest request) {
        LocationUpdateResponse response = driverLocationService.submitLocationForTrip(
                CurrentUser.id(), tripId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Verify the buyer's handover code for my trip")
    @PostMapping("/{tripId}/otp/verify")
    public ResponseEntity<ApiResponse<LogisticsJob>> verifyTripOtp(
            @PathVariable Long tripId, @RequestParam String otp) {
        DeliveryTrip trip = logisticsService.getTripForDriver(tripId, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success(
                logisticsService.verifyDeliveryOtp(trip.getDeliveryJobId(), otp)));
    }

    @Operation(summary = "File proof of delivery for my trip")
    @PostMapping("/{tripId}/proof")
    public ResponseEntity<ApiResponse<ProofOfDelivery>> fileTripProof(
            @PathVariable Long tripId, @RequestBody ProofOfDelivery proof,
            @RequestParam(required = false) String otp) {
        DeliveryTrip trip = logisticsService.getTripForDriver(tripId, CurrentUser.id());
        proof.setLogisticsJobId(trip.getDeliveryJobId());
        return ResponseEntity.ok(ApiResponse.success(
                logisticsService.createProofOfDelivery(proof, otp)));
    }

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

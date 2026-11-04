package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ForbiddenException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.DeliveryTrip;
import com.kilivana.backend.logistics.entity.ProofOfDelivery;
import com.kilivana.backend.logistics.dto.CurrentTripResponse;
import com.kilivana.backend.logistics.dto.DeliveryTripResponse;
import com.kilivana.backend.logistics.dto.LocationUpdateRequest;
import com.kilivana.backend.logistics.dto.LocationUpdateResponse;
import com.kilivana.backend.logistics.dto.ProofPhotoUploadResponse;
import com.kilivana.backend.logistics.service.DeliveryProofPhotoService;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

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
    private final DeliveryProofPhotoService deliveryProofPhotoService;

    @Operation(summary = "List my active delivery trips")
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<DeliveryTripResponse>>> getMyTrips() {
        List<DeliveryTripResponse> trips = logisticsService.getTripsForDriver(CurrentUser.id()).stream()
                .map(DeliveryTripResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    @Operation(summary = "Accept an assigned job and create a trip")
    @PostMapping("/jobs/{jobId}/accept")
    public ResponseEntity<ApiResponse<DeliveryTripResponse>> acceptJob(@PathVariable Long jobId) {
        Long driverId = CurrentUser.id();
        logisticsService.acceptJob(jobId, driverId);
        DeliveryTrip trip = logisticsService.getTripsForDriver(driverId).stream()
                .filter(active -> jobId.equals(active.getDeliveryJobId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Active trip"));
        return ResponseEntity.ok(ApiResponse.success(DeliveryTripResponse.fromEntity(trip)));
    }

    @Operation(summary = "Get a delivery trip")
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<DeliveryTripResponse>> getTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(
                DeliveryTripResponse.fromEntity(logisticsService.getTripForDriver(tripId, CurrentUser.id()))));
    }

    @Operation(summary = "Advance my trip to the next delivery status",
            description = "The picked_up action also records arrival at pickup when the job is en route.")
    @PostMapping("/{tripId}/status")
    public ResponseEntity<ApiResponse<LogisticsJob>> updateTripStatus(
            @PathVariable Long tripId, @RequestParam String status) {
        DeliveryTrip trip = logisticsService.getTripForDriver(tripId, CurrentUser.id());
        var targetStatus = com.kilivana.backend.common.enums.DeliveryStatus.from(status);
        LogisticsJob job = targetStatus == com.kilivana.backend.common.enums.DeliveryStatus.PICKED_UP
                ? logisticsService.markJobPickedUp(trip.getDeliveryJobId(), CurrentUser.id())
                : logisticsService.updateJobStatus(trip.getDeliveryJobId(), targetStatus);
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

    @Operation(summary = "Upload a proof-of-delivery photo",
            description = "Authenticated assigned driver only. Upload one JPEG, PNG, GIF or WebP image "
                    + "up to 10 MB as multipart/form-data in the required 'image' part. The response "
                    + "data.url is the stored image URL to submit as photoUrl in POST /api/v1/trips/{tripId}/proof.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Photo stored; URL returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unsupported image, image too large, or delivery not active"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Trip belongs to another driver"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Trip or delivery job not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "Image exceeds the 10 MB request limit"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Image storage failed")
    })
    @PostMapping(value = "/{tripId}/proof-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<ProofPhotoUploadResponse>> uploadProofPhoto(
            @PathVariable Long tripId,
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "JPEG, PNG, GIF or WebP image, up to 10 MB", required = true)
            @RequestPart("image") MultipartFile image) throws java.io.IOException {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(
                        deliveryProofPhotoService.upload(tripId, CurrentUser.id(), image)));
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
            description = "Returns the authenticated driver's active delivery job and its tripId.")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentTripResponse>> getCurrentTrip() {
        Long driverId = CurrentUser.id();
        Set<com.kilivana.backend.common.enums.DeliveryStatus> activeStates = driverLocationService.getActiveStates();
        var jobsById = logisticsService.getJobsByDriver(driverId).stream()
                .collect(java.util.stream.Collectors.toMap(LogisticsJob::getId, job -> job));
        CurrentTripResponse current = logisticsService.getTripsForDriver(driverId).stream()
                .filter(trip -> jobsById.containsKey(trip.getDeliveryJobId()))
                .filter(trip -> activeStates.contains(jobsById.get(trip.getDeliveryJobId()).getStatus()))
                .findFirst()
                .map(trip -> CurrentTripResponse.from(
                        jobsById.get(trip.getDeliveryJobId()), trip.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Active trip"));
        return ResponseEntity.ok(ApiResponse.success(current));
    }

    /**
     * Start tracking for a trip. The route identifier is a trip ID, not a delivery-job ID.
     */
    @Operation(summary = "Start tracking for a trip",
            description = "Accepts an assigned job and advances it to en_route_to_pickup.")
    @PostMapping("/{tripId}/tracking/start")
    public ResponseEntity<ApiResponse<Void>> startTracking(
            @Parameter(description = "ID of the trip to start tracking") @PathVariable Long tripId) {
        Long driverId = CurrentUser.id();
        DeliveryTrip trip = logisticsService.getTripForDriver(tripId, driverId);
        LogisticsJob job = logisticsService.getJobById(trip.getDeliveryJobId());
        if (!driverId.equals(job.getDriverId())) {
            throw new ForbiddenException("Only the driver assigned to this trip can start tracking");
        }

        logisticsService.startJobTracking(job.getId(), driverId);

        return ResponseEntity.ok(ApiResponse.successMessage("Tracking started successfully"));
    }
}

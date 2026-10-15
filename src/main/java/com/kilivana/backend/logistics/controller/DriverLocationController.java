package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.logistics.dto.DriverLocationResponse;
import com.kilivana.backend.logistics.dto.LocationUpdateRequest;
import com.kilivana.backend.logistics.dto.LocationUpdateResponse;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.logistics.service.TrackingAuthorizationService;
import com.kilivana.backend.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST surface for live delivery tracking. The driver pushes fixes here; the buyer, the
 * sellers on the order and an administrator read the latest position back.
 *
 * <p>WebSocket subscribers receive the same events this endpoint would otherwise require
 * polling for, so the two transports are kept in sync by the same write path.
 */
@Tag(name = "Logistics · Live Tracking",
        description = "Driver location ingestion and authorized retrieval for active deliveries.")
@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
public class DriverLocationController {

    private final DriverLocationService driverLocationService;
    private final TrackingAuthorizationService authorizationService;

    @Operation(summary = "Report a driver's location",
            description = "Records a GPS fix for the delivery the authenticated driver is running. "
                    + "The driver is identified from the JWT, never from the request body, and must be "
                    + "the one assigned to the job. Updates are accepted only while the job is in an active "
                    + "state (accepted, en route, picked up, in transit, arrived). Coordinates, speed, "
                    + "bearing and accuracy are validated; a stale timestamp is refused.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Fix accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid coordinates, stale timestamp, inactive delivery, or caller not the driver"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or expired authentication"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many updates; slow down")
    })
    @PostMapping("/{jobId}/location")
    public ResponseEntity<ApiResponse<LocationUpdateResponse>> submitLocation(
            @Parameter(description = "ID of the delivery job") @PathVariable Long jobId,
            @Valid @RequestBody LocationUpdateRequest request) {
        LocationUpdateResponse response = driverLocationService.submitLocation(CurrentUser.id(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get the latest driver location for a delivery",
            description = "Returns the driver's most recent fix. Allowed for the assigned driver, "
                    + "the buyer, any seller on the order, and an administrator. Returns 404 when no "
                    + "fix has been recorded yet, which is the right answer before the driver starts moving.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Latest fix returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or expired authentication"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorised to track this delivery"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No location recorded yet for this delivery")
    })
    @GetMapping("/{jobId}/location")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> getLocation(
            @Parameter(description = "ID of the delivery job") @PathVariable Long jobId) {
        authorizationService.ensureCanTrack(jobId, CurrentUser.id());
        DriverLocationResponse response = driverLocationService.getLatest(jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get the tracking status of a delivery",
            description = "Returns the delivery status alongside the latest fix, so a client can draw "
                    + "the right state without a second call. The status reflects the delivery job, not "
                    + "the order: a job can be in transit while the order is still confirmed.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or expired authentication"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorised to track this delivery")
    })
    @GetMapping("/{jobId}/status")
    public ResponseEntity<ApiResponse<DriverLocationResponse>> getStatus(
            @Parameter(description = "ID of the delivery job") @PathVariable Long jobId) {
        authorizationService.ensureCanTrack(jobId, CurrentUser.id());
        DriverLocationResponse response = driverLocationService.getLatest(jobId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
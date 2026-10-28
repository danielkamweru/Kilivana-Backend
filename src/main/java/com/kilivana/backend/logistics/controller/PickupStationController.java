package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.logistics.dto.PickupStationResponse;
import com.kilivana.backend.logistics.dto.SavePickupStationRequest;
import com.kilivana.backend.logistics.dto.UserPickupStationResponse;
import com.kilivana.backend.logistics.service.PickupStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing pickup stations.
 * Provides endpoints for listing available stations, saving stations to user profiles,
 * and managing user's saved pickup stations.
 */
@Tag(name = "Logistics · Pickup Stations", description = "Pickup station management for order delivery")
@RestController
@RequestMapping("/api/v1/pickup-stations")
@RequiredArgsConstructor
public class PickupStationController {

    private final PickupStationService pickupStationService;

    /**
     * Lists all available pickup stations.
     *
     * @return list of available pickup stations
     */
    @Operation(summary = "List available pickup stations",
            description = "Returns all pickup stations that are currently available for use.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pickup stations listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<PickupStationResponse>>> getAvailablePickupStations() {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.getAvailablePickupStations()));
    }

    /**
     * Lists available pickup stations by region.
     *
     * @param region the region to filter by
     * @return list of available pickup stations in the region
     */
    @Operation(summary = "List pickup stations by region",
            description = "Returns all available pickup stations in a specific region.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pickup stations listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/available/region/{region}")
    public ResponseEntity<ApiResponse<List<PickupStationResponse>>> getPickupStationsByRegion(
            @Parameter(description = "Region name", example = "Nairobi") @PathVariable String region) {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.getPickupStationsByRegion(region)));
    }

    /**
     * Gets a specific pickup station by ID.
     *
     * @param stationId the pickup station ID
     * @return the pickup station details
     */
    @Operation(summary = "Get pickup station by ID",
            description = "Returns details of a specific pickup station.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pickup station retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pickup station not found")
    })
    @GetMapping("/{stationId}")
    public ResponseEntity<ApiResponse<PickupStationResponse>> getPickupStationById(
            @Parameter(description = "Pickup station ID", example = "1") @PathVariable Long stationId) {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.getPickupStationById(stationId)));
    }

    /**
     * Checks if pickup station service is available.
     *
     * @return true if at least one station is available, false otherwise
     */
    @Operation(summary = "Check pickup station service availability",
            description = "Returns whether the pickup station service is currently available.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability status returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<Boolean>> checkPickupStationAvailability() {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.isPickupStationServiceAvailable()));
    }

    /**
     * Lists the authenticated user's saved pickup stations.
     *
     * @param userId the authenticated user's ID
     * @return list of user's saved pickup stations
     */
    @Operation(summary = "List my saved pickup stations",
            description = "Returns all pickup stations saved by the authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Saved pickup stations listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/my-stations")
    public ResponseEntity<ApiResponse<List<UserPickupStationResponse>>> getMySavedPickupStations(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.getUserSavedPickupStations(userId)));
    }

    /**
     * Saves a pickup station to the authenticated user's profile.
     *
     * @param userId  the authenticated user's ID
     * @param request the save request containing station ID and optional label
     * @return the saved pickup station
     */
    @Operation(summary = "Save pickup station",
            description = "Saves a pickup station to the authenticated user's profile for quick access.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pickup station saved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or station already saved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pickup station not found")
    })
    @PostMapping("/my-stations")
    public ResponseEntity<ApiResponse<UserPickupStationResponse>> savePickupStation(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SavePickupStationRequest request) {
        UserPickupStationResponse response = pickupStationService.savePickupStation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Deletes a saved pickup station from the authenticated user's profile.
     *
     * @param userId        the authenticated user's ID
     * @param userStationId the saved station record ID
     * @return success message
     */
    @Operation(summary = "Delete saved pickup station",
            description = "Removes a pickup station from the authenticated user's saved stations.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pickup station deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Saved pickup station not found")
    })
    @DeleteMapping("/my-stations/{userStationId}")
    public ResponseEntity<ApiResponse<Void>> deleteSavedPickupStation(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Saved pickup station ID", example = "1") @PathVariable Long userStationId) {
        pickupStationService.deleteSavedPickupStation(userId, userStationId);
        return ResponseEntity.ok(ApiResponse.successMessage("Pickup station removed successfully"));
    }

    /**
     * Sets a saved pickup station as the user's default.
     *
     * @param userId        the authenticated user's ID
     * @param userStationId the saved station record ID
     * @return the updated saved pickup station
     */
    @Operation(summary = "Set default pickup station",
            description = "Sets a saved pickup station as the user's default for future orders.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default pickup station set"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Saved pickup station not found")
    })
    @PutMapping("/my-stations/{userStationId}/default")
    public ResponseEntity<ApiResponse<UserPickupStationResponse>> setDefaultPickupStation(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Saved pickup station ID", example = "1") @PathVariable Long userStationId) {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.setDefaultPickupStation(userId, userStationId)));
    }
}

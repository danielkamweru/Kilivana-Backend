package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.logistics.dto.CreatePickupStationRequest;
import com.kilivana.backend.logistics.dto.PickupStationResponse;
import com.kilivana.backend.logistics.service.PickupStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/pickup-stations")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Logistics · Pickup Stations", description = "Platform pickup-station administration")
public class AdminPickupStationController {

    private final PickupStationService pickupStationService;

    @PostMapping
    @Operation(summary = "Create a pickup station")
    public ResponseEntity<ApiResponse<PickupStationResponse>> create(
            @Valid @RequestBody CreatePickupStationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(pickupStationService.createPickupStation(request)));
    }

    @GetMapping
    @Operation(summary = "List all pickup stations")
    public ResponseEntity<ApiResponse<List<PickupStationResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.getAllPickupStations()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pickup station")
    public ResponseEntity<ApiResponse<PickupStationResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(pickupStationService.getPickupStationById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pickup station")
    public ResponseEntity<ApiResponse<PickupStationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CreatePickupStationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                pickupStationService.updatePickupStation(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a pickup station")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        pickupStationService.deactivatePickupStation(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Pickup station deactivated"));
    }
}

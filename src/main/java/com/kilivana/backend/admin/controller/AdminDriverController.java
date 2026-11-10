package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AdminDriverResponse;
import com.kilivana.backend.admin.dto.DriverRegistrationRequest;
import com.kilivana.backend.admin.service.AdminDriverService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

/**
 * The driver roster as an administrator sees it: account, profile and delivery history in one
 * read. A driver without a profile is still listed, with their profile fields null, because a
 * registered driver who has not been onboarded yet is still a driver the panel has to show.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/drivers")
@RequiredArgsConstructor
public class AdminDriverController {

    private final AdminDriverService adminDriverService;

    @Operation(summary = "Register a driver",
            description = "Creates the driver account and profile together, from the single flat payload the "
                    + "admin panel's driver form submits. A new driver starts offline.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminDriverResponse>> registerDriver(
            @Valid @RequestBody DriverRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminDriverService.registerDriver(request)));
    }

    @Operation(summary = "Delete a driver",
            description = "Removes the driver's profile and images, then the account. Refused while the driver "
                    + "has delivery history, because that history is not disposable.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable Long userId) {
        adminDriverService.deleteDriver(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Driver deleted successfully"));
    }

    @Operation(summary = "Update a driver",
            description = "Updates the driver account and profile. Password is optional — "
                    + "omit it or send a blank value to leave the existing password unchanged.")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminDriverResponse>> updateDriver(
            @PathVariable Long userId, @Valid @RequestBody DriverRegistrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminDriverService.updateDriver(userId, request)));
    }

    @Operation(summary = "Suspend a driver",
            description = "Takes a driver off the road. The account stays; the "
                    + "roster reads them as suspended and the reason is kept on "
                    + "the profile for the panel to show. A reason is required.")
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<AdminDriverResponse>> suspendDriver(
            @PathVariable Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                adminDriverService.suspendDriver(userId, reason)));
    }

    @Operation(summary = "Reinstate a driver",
            description = "Lifts a suspension and puts the driver back online.")
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<AdminDriverResponse>> unsuspendDriver(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                adminDriverService.unsuspendDriver(userId)));
    }

    @Operation(summary = "List drivers",
            description = "Every driver account with its profile, KYC state, vehicle and delivery count.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminDriverResponse>>> listDrivers() {
        return ResponseEntity.ok(ApiResponse.success(adminDriverService.listDrivers()));
    }

    @Operation(summary = "Get a driver",
            description = "One driver by user id, in the same shape as the list.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminDriverResponse>> getDriver(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminDriverService.findDriver(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", userId))));
    }
}
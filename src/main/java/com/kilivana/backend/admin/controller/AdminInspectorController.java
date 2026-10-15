package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.InspectorProfileRequest;
import com.kilivana.backend.admin.dto.InspectorProfileResponse;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.admin.service.ProfileService;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Suspend and activate inspector accounts, mirroring the driver roster pattern. Inspectors are
 * the quality gate for the marketplace, so suspension is the main lifecycle action here — an
 * inspector who is suspended cannot be assigned to inspections. Profile edits (specialization,
 * assigned area) go through {@link ProfileService} and are exposed here for the admin panel.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/inspectors")
@RequiredArgsConstructor
public class AdminInspectorController {

    private final UserService userService;
    private final ProfileService profileService;

    @Operation(summary = "Suspend an inspector",
            description = "Suspends the inspector account. The account stays; the inspector cannot be assigned to inspections.")
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendInspector(
            @PathVariable Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(userService.suspendUser(userId, reason)));
    }

    @Operation(summary = "Unsuspend an inspector",
            description = "Reinstates a suspended inspector account.")
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<UserResponse>> unsuspendInspector(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.activateUser(userId)));
    }

    @Operation(summary = "Update an inspector profile",
            description = "Updates the inspector profile fields. Allowed for an administrator.")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<InspectorProfileResponse>> updateInspectorProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody InspectorProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.updateInspectorProfile(authenticatedUserId, userId, request)));
    }
}

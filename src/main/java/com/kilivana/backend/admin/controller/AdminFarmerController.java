package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Suspend and activate farmer accounts, mirroring the driver roster pattern.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/farmers")
@RequiredArgsConstructor
public class AdminFarmerController {

    private final UserService userService;

    @Operation(summary = "Suspend a farmer",
            description = "Suspends the farmer account. The account stays; the farmer cannot sell or interact with the platform.")
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendFarmer(
            @PathVariable Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(userService.suspendUser(userId, reason)));
    }

    @Operation(summary = "Unsuspend a farmer",
            description = "Reinstates a suspended farmer account.")
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<UserResponse>> unsuspendFarmer(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.activateUser(userId)));
    }
}

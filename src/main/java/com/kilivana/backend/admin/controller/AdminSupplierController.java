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
 * Suspend and activate supplier accounts, mirroring the driver roster pattern.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/suppliers")
@RequiredArgsConstructor
public class AdminSupplierController {

    private final UserService userService;

    @Operation(summary = "Suspend a supplier",
            description = "Suspends the supplier account. The account stays; the supplier cannot sell products.")
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspendSupplier(
            @PathVariable Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(userService.suspendUser(userId, reason)));
    }

    @Operation(summary = "Unsuspend a supplier",
            description = "Reinstates a suspended supplier account.")
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<UserResponse>> unsuspendSupplier(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.activateUser(userId)));
    }
}

package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AdminSupplierResponse;
import com.kilivana.backend.admin.dto.SupplierRegistrationRequest;
import com.kilivana.backend.admin.service.AdminSupplierService;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The supplier roster as an administrator sees it: account + profile in one flat record.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/suppliers")
@RequiredArgsConstructor
public class AdminSupplierController {

    private final AdminSupplierService adminSupplierService;

    @Operation(summary = "Register a supplier",
            description = "Creates the supplier account and profile together, from the single flat payload "
                    + "the admin panel's supplier form submits. A new supplier starts as pending.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> registerSupplier(
            @Valid @RequestBody SupplierRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminSupplierService.registerSupplier(request)));
    }

    @Operation(summary = "List suppliers",
            description = "Every supplier account with its profile, product count and status.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSupplierResponse>>> listSuppliers() {
        return ResponseEntity.ok(ApiResponse.success(adminSupplierService.listSuppliers()));
    }

    @Operation(summary = "Get a supplier",
            description = "One supplier by user id, in the same shape as the list.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> getSupplier(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminSupplierService.getSupplier(userId)));
    }

    @Operation(summary = "Edit a supplier",
            description = "Updates the supplier account and profile. Password is optional &mdash; "
                    + "omit it or send a blank value to leave the existing password unchanged.")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> updateSupplier(
            @PathVariable Long userId, @Valid @RequestBody SupplierRegistrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSupplierService.updateSupplier(userId, request)));
    }

    @Operation(summary = "Delete a supplier",
            description = "Removes the supplier's profile and account. Refused while the supplier "
                    + "has products listed.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long userId) {
        adminSupplierService.deleteSupplier(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Supplier deleted successfully"));
    }

    @Operation(summary = "Suspend a supplier",
            description = "Suspends the supplier account. The account stays; the supplier cannot "
                    + "sell products. A reason is required and is stored.")
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> suspendSupplier(
            @PathVariable Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                adminSupplierService.suspendSupplier(userId, reason)));
    }

    @Operation(summary = "Unsuspend a supplier",
            description = "Reinstates a suspended supplier account and clears the suspension reason.")
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> unsuspendSupplier(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                adminSupplierService.unsuspendSupplier(userId)));
    }
}

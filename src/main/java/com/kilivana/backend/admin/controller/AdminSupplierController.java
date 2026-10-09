package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AdminSupplierResponse;
import com.kilivana.backend.admin.dto.SupplierRegistrationRequest;
import com.kilivana.backend.admin.service.AdminSupplierService;
import com.kilivana.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The supplier roster as an administrator sees it: account + profile in one flat record.
 *
 * <p>A supplier is a seller on the marketplace. Every endpoint here is administrator-only; a
 * supplier who is not staff cannot reach this controller, because the security filter chain
 * rejects their requests with 403 before they arrive.
 */
@Tag(name = "Administration", description = "User management, audit trail and administration reporting")
@RestController
@RequestMapping("/api/v1/admin/suppliers")
@RequiredArgsConstructor
public class AdminSupplierController {

    private final AdminSupplierService adminSupplierService;

    /**
     * Registers a new supplier account and profile in a single transaction.
     *
     * <p>The login account and the business profile are created together, so a supplier is never
     * left registered without a business name or location. A new supplier starts in
     * <code>PENDING_VERIFICATION</code> and cannot list products until approved.
     *
     * @param request the flat supplier form payload, including company name, contact person and
     *               login credentials. Password is required for a new account.
     * @return the created supplier, including its generated reference code and product count.
     */
    @Operation(
        summary = "Register a supplier",
        description = "Creates the supplier account and profile together, from the single flat payload "
                + "the admin panel's supplier form submits. A new supplier starts as pending."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Supplier created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or region is not a valid Kenyan county"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email, phone number or username already registered")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> registerSupplier(
            @Valid @RequestBody SupplierRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminSupplierService.registerSupplier(request)));
    }

    /**
     * Lists every supplier account with its profile, product count and status.
     *
     * @return all suppliers, newest first.
     */
    @Operation(
        summary = "List suppliers",
        description = "Every supplier account with its profile, product count and status."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Suppliers listed")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSupplierResponse>>> listSuppliers() {
        return ResponseEntity.ok(ApiResponse.success(adminSupplierService.listSuppliers()));
    }

    /**
     * Fetches one supplier by its user id, in the same shape as the list.
     *
     * @param userId the supplier's user id
     * @return the supplier, or 404 if no such account exists
     */
    @Operation(
        summary = "Get a supplier",
        description = "One supplier by user id, in the same shape as the list."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No supplier with that user id")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> getSupplier(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminSupplierService.getSupplier(userId)));
    }

    /**
     * Updates a supplier's account and profile. The password is optional: omit it or send a
     * blank value to leave the existing password unchanged.
     *
     * @param userId  the supplier's user id
     * @param request the updated supplier fields
     * @return the updated supplier
     */
    @Operation(
        summary = "Edit a supplier",
        description = "Updates the supplier account and profile. Password is optional — "
                + "omit it or send a blank value to leave the existing password unchanged."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or region is not a valid Kenyan county"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No supplier with that user id"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email, phone number or username already registered")
    })
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> updateSupplier(
            @PathVariable Long userId, @Valid @RequestBody SupplierRegistrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSupplierService.updateSupplier(userId, request)));
    }

    /**
     * Deletes a supplier's profile and account. Refused while the supplier still has products
     * listed, because those products are part of the marketplace and cannot simply vanish.
     *
     * @param userId the supplier's user id
     * @return a success message, or 400 if the supplier has products
     */
    @Operation(
        summary = "Delete a supplier",
        description = "Removes the supplier's profile and account. Refused while the supplier "
                + "has products listed."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Supplier still has products listed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No supplier with that user id")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long userId) {
        adminSupplierService.deleteSupplier(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Supplier deleted successfully"));
    }

    /**
     * Suspends a supplier: the account stays but the supplier cannot sell. The reason is
     * stored on the account so the panel can show it later.
     *
     * @param userId the supplier's user id
     * @param reason why the supplier is being suspended; required, non-blank
     * @return the updated supplier
     */
    @Operation(
        summary = "Suspend a supplier",
        description = "Suspends the supplier account. The account stays; the supplier cannot "
                + "sell products. A reason is required and is stored."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier suspended"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A suspension reason is required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No supplier with that user id")
    })
    @PutMapping("/{userId}/suspend")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> suspendSupplier(
            @PathVariable Long userId, @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.success(
                adminSupplierService.suspendSupplier(userId, reason)));
    }

    /**
     * Reinstates a suspended supplier, clears the suspension reason and puts them back online.
     *
     * @param userId the supplier's user id
     * @return the reinstated supplier
     */
    @Operation(
        summary = "Unsuspend a supplier",
        description = "Reinstates a suspended supplier account and clears the suspension reason."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Supplier reinstated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No supplier with that user id")
    })
    @PutMapping("/{userId}/unsuspend")
    public ResponseEntity<ApiResponse<AdminSupplierResponse>> unsuspendSupplier(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(
                adminSupplierService.unsuspendSupplier(userId)));
    }
}

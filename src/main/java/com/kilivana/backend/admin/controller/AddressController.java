package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.AddressRequest;
import com.kilivana.backend.admin.dto.AddressResponse;
import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.service.AddressService;
import com.kilivana.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Address book self-service. Users read, create, update and delete their
 * own addresses, either explicitly scoped by a user id in the path or
 * implicitly through the authenticated principal. Updates and deletes only
 * succeed when the address belongs to the given user.
 */
@Tag(name = "Administration · Users & Addresses", description = "Account self-service and address book management")
@RestController
@RequestMapping({"/api/v1/users", "/api/v1"})
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * Lists every address of one user.
     *
     * @param userId the user id
     * @return the user's addresses
     */
    @Operation(summary = "List a user's addresses",
            description = "Returns every address belonging to the given user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Addresses listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid user id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddressesByUser(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddressesByUser(userId)));
    }

    /**
     * Lists the authenticated user's addresses.
     *
     * @param userId the authenticated user's id
     * @return the user's addresses
     */
    @Operation(summary = "List my addresses",
            description = "Returns every address belonging to the authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Addresses listed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getOwnAddresses(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddressesByUser(userId)));
    }

    /**
     * Creates an address for one user.
     *
     * @param userId  the user id
     * @param request the address details
     * @return the created address
     */
    @Operation(summary = "Create an address for a user",
            description = "Creates a new address and attaches it to the given user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Address created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Creates an address for the authenticated user.
     *
     * @param userId  the authenticated user's id
     * @param request the address details
     * @return the created address
     */
    @Operation(summary = "Create my address",
            description = "Creates a new address and attaches it to the authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Address created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createOwnAddress(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Updates one address of one user.
     *
     * @param userId    the user id
     * @param addressId the address id
     * @param request   the new address details
     * @return the updated address
     */
    @Operation(summary = "Update a user's address",
            description = "Replaces the details of an address, provided it belongs to the given user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found or not owned by the user")
    })
    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId,
            @Parameter(description = "Address id", example = "1") @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(addressService.updateAddress(userId, addressId, request)));
    }

    /**
     * Updates one of the authenticated user's addresses.
     *
     * @param userId    the authenticated user's id
     * @param addressId the address id
     * @param request   the new address details
     * @return the updated address
     */
    @Operation(summary = "Update my address",
            description = "Replaces the details of the authenticated user's address.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or malformed request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found or not owned by the user")
    })
    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateOwnAddress(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Address id", example = "1") @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(addressService.updateAddress(userId, addressId, request)));
    }

    /**
     * Deletes one address of one user.
     *
     * @param userId    the user id
     * @param addressId the address id
     * @return empty response with a success message
     */
    @Operation(summary = "Delete a user's address",
            description = "Removes an address, provided it belongs to the given user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found or not owned by the user")
    })
    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @Parameter(description = "User id", example = "1") @PathVariable Long userId,
            @Parameter(description = "Address id", example = "1") @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.successMessage("Address deleted successfully"));
    }

    /**
     * Deletes one of the authenticated user's addresses.
     *
     * @param userId    the authenticated user's id
     * @param addressId the address id
     * @return empty response with a success message
     */
    @Operation(summary = "Delete my address",
            description = "Removes one of the authenticated user's addresses.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid id format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found or not owned by the user")
    })
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteOwnAddress(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "Address id", example = "1") @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.successMessage("Address deleted successfully"));
    }
}

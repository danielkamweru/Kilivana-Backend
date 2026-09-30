package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.*;
import com.kilivana.backend.admin.service.ProfileService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.dto.ImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Administration-side role profiles. Driver endpoints live in
 * {@link com.kilivana.backend.logistics.controller.DriverProfileController} because a
 * driver belongs to the logistics workforce.
 */
@Tag(name = "Administration · Role Profiles",
        description = "Farmer, buyer, supplier and inspector profile management. "
                + "Each role may read and update only its own profile and upload its own images; "
                + "an administrator may manage any of these profiles. Requests for another user's "
                + "profile are rejected with 403.")
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // ---------------------------------------------------------------- farmers

    @Operation(summary = "Get a farmer profile",
            description = "Returns the farmer profile with its images. Allowed for the farmer themselves or an administrator.")
    @GetMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> getFarmerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getFarmerProfile(authenticatedUserId, userId)));
    }

    @Operation(summary = "Create a farmer profile",
            description = "Creates the farmer profile. Allowed for the farmer themselves or an administrator.")
    @PostMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> createFarmerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody FarmerProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profileService.createFarmerProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Update a farmer profile",
            description = "Updates the farmer profile. Allowed for the farmer themselves or an administrator.")
    @PutMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> updateFarmerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody FarmerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateFarmerProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Delete a farmer profile",
            description = "Deletes the farmer profile. Administrator only.")
    @DeleteMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteFarmerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        profileService.deleteFarmerProfile(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Farmer profile deleted successfully"));
    }

    @Operation(summary = "Upload farmer images",
            description = "Uploads farm photographs. Allowed for the farmer themselves or an administrator.")
    @PostMapping(value = "/farmers/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadFarmerImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = profileService.uploadFarmerProfileImage(authenticatedUserId, userId, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    @Operation(summary = "List farmer images",
            description = "Lists the farmer's images. Allowed for the farmer themselves or an administrator.")
    @GetMapping("/farmers/{userId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getFarmerImages(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        List<ImageResponse> images = profileService.getFarmerProfileImages(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @Operation(summary = "Set the primary farmer image",
            description = "Marks one image as the farmer's primary. Allowed for the farmer themselves or an administrator.")
    @PutMapping("/farmers/{userId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryFarmerImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        profileService.setPrimaryFarmerProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Primary image updated successfully"));
    }

    @Operation(summary = "Delete a farmer image",
            description = "Deletes one of the farmer's images. Allowed for the farmer themselves or an administrator.")
    @DeleteMapping("/farmers/{userId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteFarmerImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) throws IOException {
        profileService.deleteFarmerProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }

    // ----------------------------------------------------------------- buyers

    @Operation(summary = "Get a buyer profile",
            description = "Returns the buyer profile. Allowed for the buyer themselves or an administrator.")
    @GetMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<BuyerProfileResponse>> getBuyerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getBuyerProfile(authenticatedUserId, userId)));
    }

    @Operation(summary = "Create a buyer profile",
            description = "Creates the buyer profile. Allowed for the buyer themselves or an administrator.")
    @PostMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<BuyerProfileResponse>> createBuyerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody BuyerProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profileService.createBuyerProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Update a buyer profile",
            description = "Updates the buyer profile. Allowed for the buyer themselves or an administrator.")
    @PutMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<BuyerProfileResponse>> updateBuyerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody BuyerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateBuyerProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Delete a buyer profile",
            description = "Deletes the buyer profile. Administrator only.")
    @DeleteMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteBuyerProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        profileService.deleteBuyerProfile(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Buyer profile deleted successfully"));
    }

    // ------------------------------------------------------------- inspectors

    @Operation(summary = "Get an inspector profile",
            description = "Returns the inspector profile with its credential images. Allowed for the inspector themselves or an administrator.")
    @GetMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<InspectorProfileResponse>> getInspectorProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getInspectorProfile(authenticatedUserId, userId)));
    }

    @Operation(summary = "Create an inspector profile",
            description = "Creates the inspector profile. Allowed for the inspector themselves or an administrator.")
    @PostMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<InspectorProfileResponse>> createInspectorProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody InspectorProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profileService.createInspectorProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Update an inspector profile",
            description = "Updates the inspector profile. Allowed for the inspector themselves or an administrator.")
    @PutMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<InspectorProfileResponse>> updateInspectorProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody InspectorProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateInspectorProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Delete an inspector profile",
            description = "Deletes the inspector profile and its images. Administrator only.")
    @DeleteMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteInspectorProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        profileService.deleteInspectorProfile(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Inspector profile deleted successfully"));
    }

    @Operation(summary = "Upload inspector images",
            description = "Uploads inspector credential images such as a licence or badge photo. "
                    + "Allowed for the inspector themselves or an administrator.")
    @PostMapping(value = "/inspectors/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadInspectorImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = profileService.uploadInspectorProfileImage(authenticatedUserId, userId, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    @Operation(summary = "List inspector images",
            description = "Lists the inspector's credential images. Allowed for the inspector themselves or an administrator.")
    @GetMapping("/inspectors/{userId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getInspectorImages(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        List<ImageResponse> images = profileService.getInspectorProfileImages(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @Operation(summary = "Set the primary inspector image",
            description = "Marks one image as the inspector's primary. Allowed for the inspector themselves or an administrator.")
    @PutMapping("/inspectors/{userId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryInspectorImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        profileService.setPrimaryInspectorProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Primary image updated successfully"));
    }

    @Operation(summary = "Delete an inspector image",
            description = "Deletes one of the inspector's images. Allowed for the inspector themselves or an administrator.")
    @DeleteMapping("/inspectors/{userId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteInspectorImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        profileService.deleteInspectorProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }

    // ------------------------------------------------------------- suppliers

    @Operation(summary = "Get a supplier profile",
            description = "Returns the supplier profile with its images. Allowed for the supplier themselves or an administrator.")
    @GetMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<SupplierProfileResponse>> getSupplierProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getSupplierProfile(authenticatedUserId, userId)));
    }

    @Operation(summary = "Create a supplier profile",
            description = "Creates the supplier profile. Allowed for the supplier themselves or an administrator.")
    @PostMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<SupplierProfileResponse>> createSupplierProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody SupplierProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profileService.createSupplierProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Update a supplier profile",
            description = "Updates the supplier profile. Allowed for the supplier themselves or an administrator.")
    @PutMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<SupplierProfileResponse>> updateSupplierProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody SupplierProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateSupplierProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Delete a supplier profile",
            description = "Deletes the supplier profile and its images. Administrator only.")
    @DeleteMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplierProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        profileService.deleteSupplierProfile(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Supplier profile deleted successfully"));
    }

    @Operation(summary = "Upload supplier images",
            description = "Uploads business photographs. Allowed for the supplier themselves or an administrator.")
    @PostMapping(value = "/suppliers/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadSupplierImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = profileService.uploadSupplierProfileImage(authenticatedUserId, userId, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    @Operation(summary = "List supplier images",
            description = "Lists the supplier's images. Allowed for the supplier themselves or an administrator.")
    @GetMapping("/suppliers/{userId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getSupplierImages(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        List<ImageResponse> images = profileService.getSupplierProfileImages(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @Operation(summary = "Set the primary supplier image",
            description = "Marks one image as the supplier's primary. Allowed for the supplier themselves or an administrator.")
    @PutMapping("/suppliers/{userId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimarySupplierImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        profileService.setPrimarySupplierProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Primary image updated successfully"));
    }

    @Operation(summary = "Delete a supplier image",
            description = "Deletes one of the supplier's images. Allowed for the supplier themselves or an administrator.")
    @DeleteMapping("/suppliers/{userId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplierImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        profileService.deleteSupplierProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }
}

package com.kilivana.backend.logistics.controller;

import com.kilivana.backend.admin.dto.DriverProfileRequest;
import com.kilivana.backend.admin.dto.DriverProfileResponse;
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
 * Driver accounts are part of the logistics workforce, so these endpoints sit under
 * Logistics rather than Administration. A driver manages their own profile and
 * images; an admin may manage any driver's.
 */
@Tag(name = "Logistics · Driver Profiles",
        description = "Driver profile and vehicle details, including licence and vehicle images. "
                + "A driver may read and update only their own profile and upload their own images; "
                + "an administrator may manage any driver.")
@RestController
@RequestMapping("/api/v1/profiles/drivers")
@RequiredArgsConstructor
public class DriverProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Get a driver profile",
            description = "Returns the driver profile. Allowed for the driver themselves or an administrator.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getDriverProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getDriverProfile(authenticatedUserId, userId)));
    }

    @Operation(summary = "Create a driver profile",
            description = "Creates the driver profile. Allowed for the driver themselves or an administrator.")
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> createDriverProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody DriverProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profileService.createDriverProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Update a driver profile",
            description = "Updates the driver profile. Allowed for the driver themselves or an administrator.")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateDriverProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @Valid @RequestBody DriverProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateDriverProfile(authenticatedUserId, userId, request)));
    }

    @Operation(summary = "Delete a driver profile",
            description = "Deletes the driver profile and its images. Administrator only.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteDriverProfile(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        profileService.deleteDriverProfile(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Driver profile deleted successfully"));
    }

    @Operation(summary = "Upload driver images",
            description = "Uploads licence or vehicle images. Allowed for the driver themselves or an administrator.")
    @PostMapping(value = "/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadDriverImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = profileService.uploadDriverProfileImage(authenticatedUserId, userId, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    @Operation(summary = "List driver images",
            description = "Lists the driver's images. Allowed for the driver themselves or an administrator.")
    @GetMapping("/{userId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getDriverImages(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        List<ImageResponse> images = profileService.getDriverProfileImages(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @Operation(summary = "Set the primary driver image",
            description = "Marks one image as the driver's primary. Allowed for the driver themselves or an administrator.")
    @PutMapping("/{userId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryDriverImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        profileService.setPrimaryDriverProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Primary image updated successfully"));
    }

    @Operation(summary = "Delete a driver image",
            description = "Deletes one of the driver's images. Allowed for the driver themselves or an administrator.")
    @DeleteMapping("/{userId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteDriverImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) throws IOException {
        profileService.deleteDriverProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }
}

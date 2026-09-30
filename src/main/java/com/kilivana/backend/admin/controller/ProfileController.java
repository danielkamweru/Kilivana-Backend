package com.kilivana.backend.admin.controller;

import com.kilivana.backend.admin.dto.*;
import com.kilivana.backend.admin.service.ProfileService;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.dto.ImageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Administration · Role Profiles", description = "Farmer, buyer, supplier, driver and inspector profile management")
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> getFarmerProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getFarmerProfile(userId)));
    }

    @PostMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> createFarmerProfile(@PathVariable Long userId, @Valid @RequestBody FarmerProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profileService.createFarmerProfile(userId, request)));
    }

    @PutMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> updateFarmerProfile(@PathVariable Long userId, @Valid @RequestBody FarmerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateFarmerProfile(userId, request)));
    }

    @DeleteMapping("/farmers/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteFarmerProfile(@PathVariable Long userId) {
        profileService.deleteFarmerProfile(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Farmer profile deleted successfully"));
    }

    @GetMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<BuyerProfileResponse>> getBuyerProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getBuyerProfile(userId)));
    }

    @PostMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<BuyerProfileResponse>> createBuyerProfile(@PathVariable Long userId, @Valid @RequestBody BuyerProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profileService.createBuyerProfile(userId, request)));
    }

    @PutMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<BuyerProfileResponse>> updateBuyerProfile(@PathVariable Long userId, @Valid @RequestBody BuyerProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateBuyerProfile(userId, request)));
    }

    @DeleteMapping("/buyers/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteBuyerProfile(@PathVariable Long userId) {
        profileService.deleteBuyerProfile(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Buyer profile deleted successfully"));
    }

    @GetMapping("/drivers/{userId}")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getDriverProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getDriverProfile(userId)));
    }

    @PostMapping("/drivers/{userId}")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> createDriverProfile(@PathVariable Long userId, @Valid @RequestBody DriverProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profileService.createDriverProfile(userId, request)));
    }

    @PutMapping("/drivers/{userId}")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateDriverProfile(@PathVariable Long userId, @Valid @RequestBody DriverProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateDriverProfile(userId, request)));
    }

    @DeleteMapping("/drivers/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteDriverProfile(@PathVariable Long userId) {
        profileService.deleteDriverProfile(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Driver profile deleted successfully"));
    }

    @GetMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<InspectorProfileResponse>> getInspectorProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getInspectorProfile(userId)));
    }

    @PostMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<InspectorProfileResponse>> createInspectorProfile(@PathVariable Long userId, @Valid @RequestBody InspectorProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profileService.createInspectorProfile(userId, request)));
    }

    @PutMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<InspectorProfileResponse>> updateInspectorProfile(@PathVariable Long userId, @Valid @RequestBody InspectorProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateInspectorProfile(userId, request)));
    }

    @DeleteMapping("/inspectors/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteInspectorProfile(@PathVariable Long userId) {
        profileService.deleteInspectorProfile(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Inspector profile deleted successfully"));
    }

    @GetMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<SupplierProfileResponse>> getSupplierProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getSupplierProfile(userId)));
    }

    @PostMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<SupplierProfileResponse>> createSupplierProfile(@PathVariable Long userId, @Valid @RequestBody SupplierProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profileService.createSupplierProfile(userId, request)));
    }

    @PutMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<SupplierProfileResponse>> updateSupplierProfile(@PathVariable Long userId, @Valid @RequestBody SupplierProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(profileService.updateSupplierProfile(userId, request)));
    }

    @DeleteMapping("/suppliers/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplierProfile(@PathVariable Long userId) {
        profileService.deleteSupplierProfile(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Supplier profile deleted successfully"));
    }

    @PostMapping(value = "/farmers/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadFarmerImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = profileService.uploadFarmerProfileImage(authenticatedUserId, userId, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    @GetMapping("/farmers/{userId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getFarmerImages(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        List<ImageResponse> images = profileService.getFarmerProfileImages(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @PutMapping("/farmers/{userId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryFarmerImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        profileService.setPrimaryFarmerProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Primary image updated successfully"));
    }

    @DeleteMapping("/farmers/{userId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteFarmerImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) throws IOException {
        profileService.deleteFarmerProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }

    @PostMapping(value = "/suppliers/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadSupplierImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = profileService.uploadSupplierProfileImage(authenticatedUserId, userId, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    @GetMapping("/suppliers/{userId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getSupplierImages(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        List<ImageResponse> images = profileService.getSupplierProfileImages(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @DeleteMapping("/suppliers/{userId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplierImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) throws IOException {
        profileService.deleteSupplierProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }

    @PostMapping(value = "/drivers/{userId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ImageResponse>>> uploadDriverImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) throws IOException {
        List<ImageResponse> images = profileService.uploadDriverProfileImage(authenticatedUserId, userId, image, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(images));
    }

    @GetMapping("/drivers/{userId}/images")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getDriverImages(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId) {
        List<ImageResponse> images = profileService.getDriverProfileImages(authenticatedUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @DeleteMapping("/drivers/{userId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteDriverImage(
            @AuthenticationPrincipal Long authenticatedUserId,
            @PathVariable Long userId,
            @PathVariable Long imageId) throws IOException {
        profileService.deleteDriverProfileImage(authenticatedUserId, userId, imageId);
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }
}
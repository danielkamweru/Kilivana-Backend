package com.kilivana.backend.common.controller;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.dto.PublicFarmerProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for farmer profile information.
 * These endpoints are accessible without authentication for buyers viewing product pages.
 */
@Tag(name = "Public Catalogue", description = "Public farmer profile information for buyers")
@RestController
@RequestMapping("/api/v1/users/farmers")
@RequiredArgsConstructor
public class PublicFarmerController {

    private final UserRepository userRepository;

    /**
     * Returns public farmer profile information by farmer ID.
     * Only non-sensitive data is exposed: name, region, verification status, and creation date.
     *
     * @param farmerId the farmer's user ID
     * @return public farmer profile information
     */
    @Operation(summary = "Get public farmer profile",
            description = "Returns public farmer profile information. Only non-sensitive data is exposed.")
    @GetMapping("/{farmerId}")
    public ResponseEntity<ApiResponse<PublicFarmerProfileResponse>> getPublicFarmerProfile(
            @Parameter(description = "Farmer ID", example = "23") @PathVariable Long farmerId) {

        User user = userRepository.findByIdAndRole(farmerId, UserRole.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", farmerId));

        PublicFarmerProfileResponse response = PublicFarmerProfileResponse.of(
                user.getId(),
                user.getName(),
                user.getRegion(),
                user.getVerificationStatus(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

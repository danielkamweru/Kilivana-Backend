package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.SupplierProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a supplier profile with audit timestamps and images.
 */
@Data
@Builder
@Schema(description = "Supplier profile response with metadata and images")
public class SupplierProfileResponse {

    @Schema(description = "Unique identifier of the supplier profile", example = "12")
    private Long id;

    @Schema(description = "ID of the user this profile belongs to", example = "30")
    private Long userId;

    @Schema(description = "Registered business name", example = "Green Valley Farms Ltd")
    private String businessName;

    @Schema(description = "Additional details about the business operations", example = "Wholesale vegetable supplier, cold storage facility on-site")
    private String businessDetails;

    @Schema(description = "Physical location of the business", example = "Plot 45, Nakuru-Eldoret Road")
    private String location;

    @Schema(description = "Verification information or documents reference", example = "Business License #BL-2026-045")
    private String verificationInfo;

    @Schema(description = "Timestamp when the profile was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the profile was last updated", example = "2026-01-15T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Associated images (business license, facility photos, etc.)")
    private List<ImageResponse> images;

    public static SupplierProfileResponse fromEntity(SupplierProfile profile) {
        return SupplierProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .businessName(profile.getBusinessName())
                .businessDetails(profile.getBusinessDetails())
                .location(profile.getLocation())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public static SupplierProfileResponse fromEntity(SupplierProfile profile, List<ImageResponse> images) {
        return SupplierProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .businessName(profile.getBusinessName())
                .businessDetails(profile.getBusinessDetails())
                .location(profile.getLocation())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .images(images)
                .build();
    }
}
package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.FarmerProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing a farmer profile with audit timestamps and images.
 */
@Data
@Builder
@Schema(description = "Farmer profile response with metadata and images")
public class FarmerProfileResponse {

    @Schema(description = "Unique identifier of the farmer profile", example = "3")
    private Long id;

    @Schema(description = "ID of the user this profile belongs to", example = "15")
    private Long userId;

    @Schema(description = "Name of the farm", example = "Green Acres Farm")
    private String farmName;

    @Schema(description = "Physical location of the farm", example = "Kiambu County, near Thika")
    private String location;

    @Schema(description = "Additional details about the farm operations", example = "Organic vegetable farm, 50 acres, drip irrigation")
    private String farmDetails;

    @Schema(description = "Verification information or documents reference", example = "Certificate #ORG-2026-001")
    private String verificationInfo;

    @Schema(description = "Timestamp when the profile was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the profile was last updated", example = "2026-01-15T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Associated images (farm, certificates, etc.)")
    private List<ImageResponse> images;

    public static FarmerProfileResponse fromEntity(FarmerProfile profile) {
        return FarmerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .farmName(profile.getFarmName())
                .location(profile.getLocation())
                .farmDetails(profile.getFarmDetails())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public static FarmerProfileResponse fromEntity(FarmerProfile profile, List<ImageResponse> images) {
        return FarmerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .farmName(profile.getFarmName())
                .location(profile.getLocation())
                .farmDetails(profile.getFarmDetails())
                .verificationInfo(profile.getVerificationInfo())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .images(images)
                .build();
    }
}
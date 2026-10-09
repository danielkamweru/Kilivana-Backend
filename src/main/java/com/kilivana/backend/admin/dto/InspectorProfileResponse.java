package com.kilivana.backend.admin.dto;

import com.kilivana.backend.admin.entity.InspectorProfile;
import com.kilivana.backend.common.dto.ImageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing an inspector profile with audit timestamps and images.
 */
@Data
@Builder
@Schema(description = "Inspector profile response with metadata and images")
public class InspectorProfileResponse {

    @Schema(description = "Unique identifier of the inspector profile", example = "7")
    private Long id;

    @Schema(description = "ID of the user this profile belongs to", example = "25")
    private Long userId;

    @Schema(description = "Details about the inspector's background and experience", example = "Former KEBS officer with 10 years produce inspection experience")
    private String inspectorDetails;

    @Schema(description = "What the inspector is qualified to check", example = "Tea & Coffee")
    private String specialization;

    @Schema(description = "Geographic area assigned to the inspector", example = "Central Kenya Region")
    private String assignedArea;

    @Schema(description = "Current status of the inspector", example = "ACTIVE")
    private String status;

    @Schema(description = "Timestamp when the profile was created", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the profile was last updated", example = "2026-01-15T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Associated images (ID badge, certificates, etc.)")
    private List<ImageResponse> images;

    public static InspectorProfileResponse fromEntity(InspectorProfile profile) {
        return fromEntity(profile, null);
    }

    public static InspectorProfileResponse fromEntity(InspectorProfile profile, List<ImageResponse> images) {
        return InspectorProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .inspectorDetails(profile.getInspectorDetails())
                .specialization(profile.getSpecialization())
                .assignedArea(profile.getAssignedArea())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .images(images)
                .build();
    }
}
package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.InspectionResult;
import com.kilivana.backend.common.enums.InspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing an inspection with audit timestamps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inspection response with metadata")
public class InspectionResponse {

    @Schema(description = "Unique identifier of the inspection", example = "25")
    private Long id;

    @Schema(description = "ID of the inspector who performed the inspection", example = "7")
    private Long inspectorId;

    @Schema(description = "Type of entity inspected (e.g., FARMER, SUPPLIER, PRODUCT)", example = "FARMER")
    private String targetType;

    @Schema(description = "ID of the entity that was inspected", example = "3")
    private Long targetId;

    @Schema(description = "Current status of the inspection", example = "COMPLETED")
    private InspectionStatus status;

    @Schema(description = "Result of the inspection", example = "PASSED")
    private InspectionResult result;

    @Schema(description = "Inspector notes and observations", example = "Farm meets organic standards, good record keeping")
    private String notes;

    @Schema(description = "Comma-separated URLs to evidence photos/documents", example = "https://storage.example.com/inspections/3/photo1.jpg")
    private String evidenceUrls;

    @Schema(description = "Timestamp when the inspection was performed", example = "2026-01-20T14:30:00")
    private LocalDateTime inspectedAt;

    @Schema(description = "Timestamp when the inspection record was created", example = "2026-01-20T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the inspection record was last updated", example = "2026-01-20T14:30:00")
    private LocalDateTime updatedAt;

    public static InspectionResponse fromEntity(com.kilivana.backend.admin.entity.Inspection inspection) {
        return InspectionResponse.builder()
                .id(inspection.getId())
                .inspectorId(inspection.getInspectorId())
                .targetType(inspection.getTargetType())
                .targetId(inspection.getTargetId())
                .status(inspection.getStatus())
                .result(inspection.getResult())
                .notes(inspection.getNotes())
                .evidenceUrls(inspection.getEvidenceUrls())
                .inspectedAt(inspection.getInspectedAt())
                .createdAt(inspection.getCreatedAt())
                .updatedAt(inspection.getUpdatedAt())
                .build();
    }
}

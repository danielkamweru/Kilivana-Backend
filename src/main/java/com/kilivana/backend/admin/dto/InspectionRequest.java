package com.kilivana.backend.admin.dto;

import com.kilivana.backend.common.enums.InspectionResult;
import com.kilivana.backend.common.enums.InspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request payload for creating or updating an inspection.
 */
/**
 * Request payload for creating or updating an inspection. The {@code result} is optional on
 * create and filled in when the inspector records their finding; {@code evidenceUrls} is the
 * comma-separated mirror of the evidence images table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update an inspection")
public class InspectionRequest {

    @NotNull(message = "Inspector ID is required")
    @Schema(description = "ID of the inspector performing the inspection", example = "7")
    private Long inspectorId;

    @NotNull(message = "Target type is required")
    @Schema(description = "Type of entity being inspected (e.g., FARMER, SUPPLIER, PRODUCT)", example = "FARMER")
    private String targetType;

    @NotNull(message = "Target ID is required")
    @Schema(description = "ID of the entity being inspected", example = "3")
    private Long targetId;

    @NotNull(message = "Status is required")
    @Schema(description = "Current status of the inspection", example = "IN_PROGRESS")
    private InspectionStatus status;

    @Schema(description = "Result of the inspection", example = "PASSED")
    private InspectionResult result;

    @Schema(description = "Inspector notes and observations", example = "Farm meets organic standards, good record keeping")
    private String notes;

    @Schema(description = "Comma-separated URLs to evidence photos/documents", example = "https://storage.example.com/inspections/3/photo1.jpg,https://storage.example.com/inspections/3/photo2.jpg")
    private String evidenceUrls;
}

package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO representing a crop record, including its association to a farm and farmer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO representing a crop record with farm and farmer associations")
public class CropResponse {
    @Schema(description = "Unique identifier of the crop record", example = "101")
    private Long id;

    @Schema(description = "Identifier of the farm where the crop is planted", example = "50")
    private Long farmId;

    @Schema(description = "Name of the farm where the crop is planted", example = "Green Valley Farm")
    private String farmName;

    @Schema(description = "Identifier of the farmer who owns the crop", example = "12")
    private Long farmerId;

    @Schema(description = "Name of the farmer who owns the crop", example = "John Mutua")
    private String farmerName;

    @Schema(description = "Summary of the crop type", example = "Maize (CEREAL)")
    private CropTypeSummary cropType;

    @Schema(description = "Variety of the crop", example = "BH540")
    private String variety;

    @Schema(description = "Area allocated to the crop in acres", example = "2.5")
    private Double areaAcres;

    @Schema(description = "Current status of the crop", example = "PLANTED")
    private CropStatus status;

    @Schema(description = "Date when the crop was planted", example = "2024-03-15")
    private LocalDate plantingDate;

    @Schema(description = "Expected date of harvest", example = "2024-09-15")
    private LocalDate expectedHarvestDate;

    @Schema(description = "Expected yield of the crop in kilograms", example = "1500")
    private Integer expectedYieldKg;

    @Schema(description = "Timestamp when the crop record was created", example = "2024-03-15T10:30:00")
    private LocalDateTime createdAt;

    /**
     * Nested summary of a crop type, including its category.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary of a crop type including its category")
    public static class CropTypeSummary {
        @Schema(description = "Unique identifier of the crop type", example = "3")
        private Long id;

        @Schema(description = "Name of the crop type", example = "Maize")
        private String name;

        @Schema(description = "Category of the crop type", example = "CEREAL")
        private CropCategory category;
    }
}

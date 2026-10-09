package com.kilivana.backend.farm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO summarizing aggregate crop statistics grouped by crop type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO summarizing aggregate crop statistics grouped by crop type")
public class CropSummaryDto {
    @Schema(description = "Identifier of the crop type", example = "3")
    private Long cropTypeId;

    @Schema(description = "Name of the crop type", example = "Maize")
    private String name;

    @Schema(description = "Number of farms growing this crop type", example = "12")
    private Long farmCount;

    @Schema(description = "Total area across all farms for this crop type in acres", example = "250.5")
    private Double totalAreaAcres;

    @Schema(description = "Expected total yield for this crop type in kilograms", example = "50000")
    private Long expectedYieldKg;
}

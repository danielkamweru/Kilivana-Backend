package com.kilivana.backend.farm.dto;

import com.kilivana.backend.common.enums.CropCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a crop type definition used for cataloging crops.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing a crop type definition")
public class CropTypeDto {
    @Schema(description = "Unique identifier of the crop type", example = "3")
    private Long id;

    @Schema(description = "Name of the crop type", example = "Maize")
    private String name;

    @Schema(description = "Category of the crop type", example = "CEREAL")
    private CropCategory category;

    @Schema(description = "Whether the crop type is active and available for use", example = "true")
    private boolean active;
}

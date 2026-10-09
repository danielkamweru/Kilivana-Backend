package com.kilivana.backend.farm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an image associated with a farm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing an image associated with a farm")
public class FarmImageResponse {
    @Schema(description = "Unique identifier of the farm image", example = "1")
    private Long id;

    @Schema(description = "URL of the image", example = "https://example.com/images/farm1.jpg")
    private String url;

    @Schema(description = "Whether this image is the primary image for the farm", example = "true")
    private Boolean isPrimary;
}

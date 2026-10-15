package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One image attached to a product listing, as the panel reads it: the
 * display URL, the asset manager identifiers, and its place in the gallery.
 */
@Schema(description = "Product image response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    @Schema(description = "Image ID", example = "9")
    private Long id;
    @Schema(description = "URL the panel renders for this image", example = "https://cdn.example.com/img/9.jpg")
    private String url;
    @Schema(description = "Asset manager public ID for this image", example = "prod_25_img_1")
    private String publicId;
    @Schema(description = "Asset manager asset ID for this image", example = "aif_9f3c2a")
    private String assetId;
    @Schema(description = "Display order of this image in the gallery", example = "1")
    private Integer sortOrder;
    @Schema(description = "Whether this is the primary product image", example = "true")
    private Boolean isPrimary;
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
}
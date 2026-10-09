package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.SellerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response representing a product category.
 */
@Schema(description = "Product category response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    @Schema(description = "Category ID", example = "5")
    private Long id;

    @Schema(description = "Category name", example = "Fresh Vegetables")
    private String name;

    @Schema(description = "Type of seller this category belongs to", example = "FARMER")
    private SellerType type;

    @Schema(description = "Whether the category is active", example = "true")
    private Boolean active;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T11:45:00")
    private LocalDateTime updatedAt;
}

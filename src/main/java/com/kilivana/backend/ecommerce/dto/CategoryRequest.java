package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.SellerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create or update a product category.
 */
@Schema(description = "Request to create or update a product category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @Schema(description = "Category name", example = "Fresh Vegetables")
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Schema(description = "Type of seller this category belongs to", example = "FARMER")
    @NotNull(message = "Type is required")
    private SellerType type;
}

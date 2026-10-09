package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to add or update a cart item.
 */
@Schema(description = "Request to add or update a cart item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {

    @Schema(description = "ID of the product to add to cart", example = "25")
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Schema(description = "Quantity of the product", example = "3")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}

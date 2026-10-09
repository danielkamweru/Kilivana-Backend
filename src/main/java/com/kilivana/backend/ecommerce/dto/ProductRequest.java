package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.SellerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Everything needed to create or update a product listing. The category can
 * be given either by id or by name; the panel sends whichever it has.
 */
@Schema(description = "Product request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @Schema(description = "Seller user ID who owns the listing", example = "12")
    private Long sellerId;

    @Schema(description = "Type of seller the listing belongs to", example = "FARMER")
    private SellerType sellerType;

    @Schema(description = "Category ID the listing belongs to", example = "5")
    private Long categoryId;

    @Size(max = 100, message = "Category name must be at most 100 characters")
    @Schema(description = "Category name, used when no category ID is supplied", example = "Fresh Vegetables")
    private String category;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Schema(description = "Product name", example = "Organic Tomatoes")
    private String name;

    @NotBlank(message = "Description is required")
    @Schema(description = "Product description", example = "Fresh organic tomatoes grown on our farm")
    private String description;

    @NotBlank(message = "Unit is required")
    @Schema(description = "Unit of measurement", example = "kg")
    private String unit;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Schema(description = "Price per unit", example = "1200.50")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Schema(description = "Stock quantity available", example = "50")
    private Integer stockQty;

    @NotNull(message = "Minimum order quantity is required")
    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    @Schema(description = "Minimum quantity a buyer can order", example = "1")
    private Integer minimumOrderQty;
}

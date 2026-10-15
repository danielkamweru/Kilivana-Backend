package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A line of an order as it is read back, with the display fields the panel shows.
 */
@Schema(description = "Order item response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    @Schema(description = "Order item ID", example = "50")
    private Long id;

    @Schema(description = "Product ID", example = "25")
    private Long productId;

    @Schema(description = "Seller ID", example = "12")
    private Long sellerId;

    @Schema(description = "Product name at time of purchase", example = "Organic Tomatoes")
    private String productName;

    @Schema(description = "Unit of measurement", example = "kg")
    private String unit;

    @Schema(description = "Quantity ordered", example = "3")
    private Integer quantity;

    @Schema(description = "Unit price at time of purchase", example = "1200.50")
    private BigDecimal unitPrice;

    @Schema(description = "Line subtotal (quantity * unitPrice)", example = "3601.50")
    private BigDecimal subtotal;
}

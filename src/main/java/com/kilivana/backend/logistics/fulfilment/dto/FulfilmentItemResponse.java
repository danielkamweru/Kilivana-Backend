package com.kilivana.backend.logistics.fulfilment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One line item within a seller fulfilment.
 */
@Schema(description = "One order item within a seller fulfilment.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfilmentItemResponse {

    @Schema(description = "Order item ID", example = "50")
    private Long orderItemId;

    @Schema(description = "Product ID", example = "25")
    private Long productId;

    @Schema(description = "Product name at purchase", example = "Organic Tomatoes")
    private String productName;

    @Schema(description = "Unit of measurement", example = "kg")
    private String unit;

    @Schema(description = "Quantity ordered", example = "3")
    private Integer quantity;

    @Schema(description = "Unit price", example = "1200.50")
    private BigDecimal unitPrice;

    @Schema(description = "Line subtotal", example = "3601.50")
    private BigDecimal subtotal;
}

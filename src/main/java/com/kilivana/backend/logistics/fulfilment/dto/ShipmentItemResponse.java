package com.kilivana.backend.logistics.fulfilment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One item allocated to a shipment.
 */
@Schema(description = "One order item allocated to a shipment.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentItemResponse {

    @Schema(description = "Shipment item ID", example = "200")
    private Long id;

    @Schema(description = "Order item ID", example = "50")
    private Long orderItemId;

    @Schema(description = "Product ID", example = "25")
    private Long productId;

    @Schema(description = "Product name", example = "Organic Tomatoes")
    private String productName;

    @Schema(description = "Allocated quantity", example = "3")
    private Integer allocatedQuantity;

    @Schema(description = "Unit price", example = "1200.50")
    private BigDecimal unitPrice;
}

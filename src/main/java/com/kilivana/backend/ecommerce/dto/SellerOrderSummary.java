package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Seller-scoped order summary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerOrderSummary {
    @Schema(description = "Order ID", example = "100")
    private Long id;
    @Schema(description = "Order status", example = "CONFIRMED")
    private String status;
    @Schema(description = "Seller's portion total", example = "3601.50")
    private BigDecimal total;
    @Schema(description = "Order creation timestamp", example = "2026-10-26T12:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Number of line items belonging to this seller", example = "3")
    private Integer itemCount;
    @Schema(description = "Line items belonging to this seller")
    private List<SellerOrderItem> items;

    @Schema(description = "A seller's line item within an order")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerOrderItem {
        @Schema(description = "Product ID", example = "25")
        private Long productId;
        @Schema(description = "Product name at time of purchase", example = "Organic Tomatoes")
        private String productName;
        @Schema(description = "Unit of measurement", example = "kg")
        private String unit;
        @Schema(description = "Quantity ordered", example = "3")
        private Integer quantity;
        @Schema(description = "Unit price at time of purchase", example = "1200.50")
        private BigDecimal unitPrice;
        @Schema(description = "Line subtotal", example = "3601.50")
        private BigDecimal subtotal;
    }
}

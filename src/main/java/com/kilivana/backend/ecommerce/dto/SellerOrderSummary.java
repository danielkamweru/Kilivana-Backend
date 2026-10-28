package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}

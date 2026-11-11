package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Global order statistics for order management")
public class OrderStatsResponse {

    @Schema(description = "Total number of orders")
    private Long totalOrders;

    @Schema(description = "Orders with status PLACED")
    private Long placed;

    @Schema(description = "Orders with status CONFIRMED")
    private Long confirmed;

    @Schema(description = "Orders with status PROCESSING or IN_TRANSIT")
    private Long processing;

    @Schema(description = "Orders with status DELIVERED or COMPLETED")
    private Long delivered;

    @Schema(description = "Orders with status CANCELLED")
    private Long cancelled;
}

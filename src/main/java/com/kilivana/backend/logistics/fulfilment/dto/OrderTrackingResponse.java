package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Buyer-facing order tracking response: one order with its shipments.
 */
@Schema(description = "Buyer-facing order tracking: one order with shipment details.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {

    @Schema(description = "Order ID", example = "100")
    private Long orderId;

    @Schema(description = "Order reference code", example = "ORD-00100")
    private String orderCode;

    @Schema(description = "Order status", example = "IN_TRANSIT")
    private String orderStatus;

    @Schema(description = "Overall fulfilment status", example = "PARTIALLY_FULFILLED")
    private String fulfilmentStatus;

    @Schema(description = "Shipments for this order", example = "[...]")
    private List<ShipmentSummaryResponse> shipments;

    @Schema(description = "Assigned driver name, if any", example = "John Mwangi")
    private String driverName;

    @Schema(description = "Assigned driver phone, if any", example = "+254712345678")
    private String driverPhone;

    @Schema(description = "Estimated delivery time, if available", example = "2024-01-15T14:30:00")
    private LocalDateTime estimatedDelivery;
}

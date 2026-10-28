package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response containing a delivery fee quote for checkout.
 *
 * <p>The quote includes a unique ID that must be used when creating the order.
 * The quote expires after a short time to ensure prices remain current.
 * Supports both door delivery and pickup station delivery.
 */
@Schema(description = "Delivery fee quote response")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryQuoteResponse {

    @Schema(description = "Unique quote identifier", example = "opaque-id")
    private String quoteId;

    @Schema(description = "Delivery method: DOOR_DELIVERY or PICKUP_STATION", example = "DOOR_DELIVERY")
    private String deliveryMethod;

    @Schema(description = "Delivery address ID (for DOOR_DELIVERY)", example = "8")
    private Long addressId;

    @Schema(description = "Pickup station ID (for PICKUP_STATION)", example = "5")
    private Long pickupStationId;

    @Schema(description = "Cart subtotal (sum of item prices)", example = "3601.50")
    private BigDecimal subtotal;

    @Schema(description = "Calculated delivery fee", example = "150.00")
    private BigDecimal deliveryFee;

    @Schema(description = "Total amount (subtotal + delivery fee)", example = "3751.50")
    private BigDecimal total;

    @Schema(description = "Currency code", example = "KES")
    private String currency;

    @Schema(description = "Quote expiration timestamp", example = "2026-10-26T12:30:00Z")
    private Instant expiresAt;
}

package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to calculate a delivery fee quote for checkout.
 * Supports both door delivery (using addressId) and pickup station delivery (using pickupStationId).
 */
@Schema(description = "Delivery fee quote request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryQuoteRequest {

    @Schema(description = "Delivery method: DOOR_DELIVERY or PICKUP_STATION", example = "DOOR_DELIVERY")
    @NotNull(message = "Delivery method is required")
    private String deliveryMethod;

    @Schema(description = "Delivery address ID (required for DOOR_DELIVERY)", example = "8")
    private Long addressId;

    @Schema(description = "Pickup station ID (required for PICKUP_STATION)", example = "5")
    private Long pickupStationId;
}

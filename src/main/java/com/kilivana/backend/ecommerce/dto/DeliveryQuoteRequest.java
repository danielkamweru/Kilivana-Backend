package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to calculate a delivery fee quote for checkout.
 *
 * <p>Supports door delivery (using {@code addressId}) and pickup delivery. For pickup
 * delivery the buyer simply types a one-time location — there is no platform station
 * to select, since stations are configured by an operator in the database.
 */
@Schema(description = "Delivery fee quote request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryQuoteRequest {

    @Schema(description = "Delivery method: DOOR_DELIVERY or PICKUP_STATION", example = "PICKUP_STATION")
    @NotNull(message = "Delivery method is required")
    private String deliveryMethod;

    @Schema(description = "Delivery address ID (required for DOOR_DELIVERY)", example = "8")
    private Long addressId;

    @Schema(description = "One-time pickup location entered by the buyer (required for PICKUP_STATION)",
            example = "Outside Westlands Mall, near the main entrance")
    @Size(max = 500, message = "Pickup location must be at most 500 characters")
    private String pickupLocation;
}

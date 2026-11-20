package com.kilivana.backend.logistics.fulfilment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to create route stops for a shipment.
 */
@Schema(description = "Request to create route stops for a shipment.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStopsCreateRequest {

    @Schema(description = "Seller IDs for pickup stops, in route order", example = "[28, 45]")
    @NotEmpty(message = "At least one pickup seller is required")
    private List<Long> pickupSellerIds;

    @Schema(description = "Buyer delivery address", example = "123 Mombasa Road, Nairobi")
    @NotNull(message = "Delivery address is required")
    private String deliveryAddress;

    @Schema(description = "Delivery latitude", example = "-1.2921")
    private Double deliveryLatitude;

    @Schema(description = "Delivery longitude", example = "36.8219")
    private Double deliveryLongitude;
}

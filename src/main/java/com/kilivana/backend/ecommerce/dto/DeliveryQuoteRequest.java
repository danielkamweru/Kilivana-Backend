package com.kilivana.backend.ecommerce.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to calculate a delivery fee quote for checkout.
 */
@Schema(description = "Delivery fee quote request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryQuoteRequest {

    @Schema(description = "Delivery address ID", example = "8")
    @NotNull(message = "Address ID is required")
    private Long addressId;
}

package com.kilivana.backend.logistics.fulfilment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to cancel a shipment.
 */
@Schema(description = "Request to cancel a shipment.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCancelRequest {

    @Schema(description = "Cancellation reason", example = "Driver vehicle broke down")
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}

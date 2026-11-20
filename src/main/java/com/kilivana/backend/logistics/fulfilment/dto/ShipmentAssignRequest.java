package com.kilivana.backend.logistics.fulfilment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to assign or reassign a driver to a shipment.
 */
@Schema(description = "Request to assign or reassign a driver to a shipment.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentAssignRequest {

    @Schema(description = "Driver user ID", example = "31")
    @NotNull(message = "Driver ID is required")
    private Long driverId;
}

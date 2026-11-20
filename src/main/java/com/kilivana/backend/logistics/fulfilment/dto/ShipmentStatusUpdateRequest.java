package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for a driver to update shipment status.
 */
@Schema(description = "Request for a driver to update shipment status.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusUpdateRequest {

    @Schema(description = "New shipment status", example = "PICKING_UP")
    private ShipmentStatus status;

    @Schema(description = "Driver user ID", example = "31")
    private Long driverId;
}

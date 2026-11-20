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
 * Shipment summary for buyer tracking.
 */
@Schema(description = "Shipment summary for buyer order tracking.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentSummaryResponse {

    @Schema(description = "Shipment ID", example = "7")
    private Long id;

    @Schema(description = "Shipment reference code", example = "SHIP-00042")
    private String code;

    @Schema(description = "Shipment status", example = "IN_TRANSIT")
    private ShipmentStatus status;

    @Schema(description = "Driver name, if assigned", example = "John Mwangi")
    private String driverName;

    @Schema(description = "Stops in route sequence", example = "[...]")
    private List<ShipmentStopResponse> stops;

    @Schema(description = "Estimated delivery time", example = "2024-01-15T14:30:00")
    private LocalDateTime estimatedDelivery;
}

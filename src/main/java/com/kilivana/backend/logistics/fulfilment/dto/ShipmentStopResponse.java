package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.StopStatus;
import com.kilivana.backend.logistics.fulfilment.enums.StopType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One stop along a shipment route.
 */
@Schema(description = "One stop along a shipment route (pickup or delivery).")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStopResponse {

    @Schema(description = "Stop ID", example = "15")
    private Long id;

    @Schema(description = "Stop type: PICKUP or DELIVERY", example = "PICKUP")
    private StopType stopType;

    @Schema(description = "Farmer or buyer reference ID", example = "28")
    private Long partyId;

    @Schema(description = "Stop address", example = "Green Valley Farm, Kiambu")
    private String address;

    @Schema(description = "Latitude", example = "-1.1234")
    private Double latitude;

    @Schema(description = "Longitude", example = "36.5678")
    private Double longitude;

    @Schema(description = "Position in route sequence", example = "1")
    private Integer stopSequence;

    @Schema(description = "Stop status", example = "COMPLETED")
    private StopStatus status;

    @Schema(description = "Collection or delivery instructions", example = "Knock on the farm gate")
    private String instructions;

    @Schema(description = "When driver arrived", example = "2024-01-15T10:15:00")
    private LocalDateTime arrivedAt;

    @Schema(description = "When driver completed", example = "2024-01-15T10:45:00")
    private LocalDateTime completedAt;

    @Schema(description = "Creation timestamp", example = "2024-01-15T08:30:00")
    private LocalDateTime createdAt;
}

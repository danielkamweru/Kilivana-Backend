package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.StopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for a driver to mark arrival at a shipment stop.
 */
@Schema(description = "Request for a driver to mark arrival at a shipment stop.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopArriveRequest {

    @Schema(description = "Stop ID", example = "15")
    private Long stopId;

    @Schema(description = "Driver user ID", example = "31")
    private Long driverId;

    @Schema(description = "Latitude at arrival", example = "-1.1234")
    private Double latitude;

    @Schema(description = "Longitude at arrival", example = "36.5678")
    private Double longitude;

    @Schema(description = "Notes, e.g. 'arrived at farm gate'", example = "Arrived at farm gate")
    private String note;

    @Schema(description = "Target status (ARRIVED)", example = "ARRIVED")
    private StopStatus status;
}

package com.kilivana.backend.logistics.fulfilment.dto;

import com.kilivana.backend.logistics.fulfilment.enums.StopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for a driver to mark completion of a shipment stop.
 */
@Schema(description = "Request for a driver to mark completion of a shipment stop.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StopCompleteRequest {

    @Schema(description = "Stop ID", example = "15")
    private Long stopId;

    @Schema(description = "Driver user ID", example = "31")
    private Long driverId;

    @Schema(description = "Latitude at completion", example = "-1.1234")
    private Double latitude;

    @Schema(description = "Longitude at completion", example = "36.5678")
    private Double longitude;

    @Schema(description = "Notes, e.g. 'collected 3 crates of tomatoes'", example = "Collected 3 crates of tomatoes")
    private String note;

    @Schema(description = "Target status (COMPLETED)", example = "COMPLETED")
    private StopStatus status;
}

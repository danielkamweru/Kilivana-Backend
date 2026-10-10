package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Pushed to subscribers when a delivery stops accepting live positions: it has been
 * delivered, cancelled or failed. Lets the client stop its own animation and switch to a
 * static "journey complete" view without polling for a fix that will never come.
 */
@Schema(description = "Pushed when a delivery stops accepting live positions.")
@Data
public class TrackingStoppedEvent {

    private Long tripId;
    private Long orderId;
    private Long driverId;
    private String status;
    private String message;
}
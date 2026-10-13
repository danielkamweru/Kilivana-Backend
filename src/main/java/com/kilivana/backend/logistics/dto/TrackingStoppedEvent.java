package com.kilivana.backend.logistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pushed to subscribers when a delivery stops accepting live positions: it has been
 * delivered, cancelled or failed. Lets the client stop its own animation and switch to a
 * static "journey complete" view without polling for a fix that will never come.
 */
@Schema(description = "Pushed when a delivery stops accepting live positions.")
public class TrackingStoppedEvent {

    /** ID of the delivery job whose tracking has ended. */
    private Long tripId;
    /** ID of the order this delivery job is fulfilling. */
    private Long orderId;
    /** ID of the driver who was running the delivery. */
    private Long driverId;
    /** Terminal delivery status, e.g. "delivered", "cancelled" or "failed". */
    private String status;
    /** Human-readable notice for the client to render. */
    private String message;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
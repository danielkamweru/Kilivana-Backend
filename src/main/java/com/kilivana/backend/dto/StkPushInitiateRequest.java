package com.kilivana.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Request to initiate an M-PESA STK Push for an order.
 *
 * <p>Used by farmers and suppliers to prompt the buyer for payment.
 * The amount is calculated server-side from the order's outstanding balance
 * — the client cannot supply an arbitrary amount.
 */
@Schema(description = "Request to initiate an M-PESA STK Push for an order")
public class StkPushInitiateRequest {

    @Schema(description = "Order ID to pay for", example = "100", required = true)
    @NotNull(message = "Order ID is required")
    private Long orderId;

    /**
     * Buyer's phone number in 2547XXXXXXXX format.
     * If not provided, the buyer's registered phone is used.
     */
    @Schema(description = "Buyer's phone in 2547XXXXXXXX format (optional; uses buyer's registered phone if omitted)", example = "254708374149")
    @Pattern(regexp = "^254[17]\\d{8}$", message = "Phone must be in 2547XXXXXXXX or 2541XXXXXXXX format")
    private String phoneNumber;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
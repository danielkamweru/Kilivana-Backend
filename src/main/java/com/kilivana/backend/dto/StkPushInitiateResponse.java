package com.kilivana.backend.dto;

import com.kilivana.backend.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response after initiating an STK Push for an order.
 *
 * <p>Contains the payment record ID and the Daraja STK Push response.
 * The payment is initially PENDING; final outcome arrives via the callback.
 */
@Schema(description = "Response after initiating an STK Push for an order")
public class StkPushInitiateResponse {

    @Schema(description = "Payment record ID created for this attempt", example = "123")
    private Long paymentId;

    @Schema(description = "Order ID this payment is for", example = "100")
    private Long orderId;

    @Schema(description = "Payment amount in KES", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Payment status (initially PENDING)", example = "PENDING")
    private String status;

    @Schema(description = "Daraja STK Push response")
    private StkPushResponse darajaResponse;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StkPushResponse getDarajaResponse() {
        return darajaResponse;
    }

    public void setDarajaResponse(StkPushResponse darajaResponse) {
        this.darajaResponse = darajaResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
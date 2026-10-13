package com.kilivana.backend.ecommerce.dto;

import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A payment as the panel reads it: the order it settles, how it was made,
 * the provider's reference, and where the money sits.
 */
@Schema(description = "Payment response")
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "33")
    private Long id;
    @Schema(description = "Order ID this payment settles", example = "100")
    private Long orderId;
    @Schema(description = "Payment method used", example = "MPESA")
    private PaymentMethod method;
    @Schema(description = "Payment transaction reference from the provider", example = "QF8H2K1A")
    private String reference;
    @Schema(description = "Amount paid, in the order currency", example = "3751.50")
    private BigDecimal amount;
    @Schema(description = "Current payment status", example = "HELD")
    private PaymentStatus status;
    @Schema(description = "Timestamp when the payment was made", example = "2024-01-15T11:00:00")
    private LocalDateTime paidAt;
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Last update timestamp", example = "2024-01-15T11:45:00")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static PaymentResponseBuilder builder() {
        return new PaymentResponseBuilder();
    }

    public static class PaymentResponseBuilder {
        private Long id;
        private Long orderId;
        private PaymentMethod method;
        private String reference;
        private BigDecimal amount;
        private PaymentStatus status;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public PaymentResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentResponseBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentResponseBuilder method(PaymentMethod method) {
            this.method = method;
            return this;
        }

        public PaymentResponseBuilder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public PaymentResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentResponseBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentResponseBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public PaymentResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PaymentResponse build() {
            PaymentResponse paymentResponse = new PaymentResponse();
            paymentResponse.id = this.id;
            paymentResponse.orderId = this.orderId;
            paymentResponse.method = this.method;
            paymentResponse.reference = this.reference;
            paymentResponse.amount = this.amount;
            paymentResponse.status = this.status;
            paymentResponse.paidAt = this.paidAt;
            paymentResponse.createdAt = this.createdAt;
            paymentResponse.updatedAt = this.updatedAt;
            return paymentResponse;
        }
    }
}

package com.kilivana.backend.ecommerce.entity;

import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.PaymentMethod;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A payment attempt recorded against an order.
 *
 * <p>The order's {@code paymentStatus} is a derived view of its payment rows
 * and is kept in sync by the payment service. {@code paidAt} records when the
 * money actually arrived (escrow steps must not re-stamp it). The {@code reference}
 * is the provider's transaction ID (e.g. M-Pesa confirmation code).
 *
 * <p>Daraja-specific fields are populated when the payment is initiated via
 * M-PESA STK Push. They enable callback reconciliation and audit without
 * requiring a separate table.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentMethod method;

    @Column(nullable = false)
    private String reference;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Daraja (M-PESA STK Push) specific fields
    // Populated at STK initiation and callback for reconciliation
    private String merchantRequestId;
    private String checkoutRequestId;
    private String phoneNumber;
    private String mpesaReceiptNumber;
    private LocalDateTime transactionDate;
    private String callbackStatus; // PENDING, RECEIVED, PROCESSED, FAILED

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

    public String getMerchantRequestId() {
        return merchantRequestId;
    }

    public void setMerchantRequestId(String merchantRequestId) {
        this.merchantRequestId = merchantRequestId;
    }

    public String getCheckoutRequestId() {
        return checkoutRequestId;
    }

    public void setCheckoutRequestId(String checkoutRequestId) {
        this.checkoutRequestId = checkoutRequestId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMpesaReceiptNumber() {
        return mpesaReceiptNumber;
    }

    public void setMpesaReceiptNumber(String mpesaReceiptNumber) {
        this.mpesaReceiptNumber = mpesaReceiptNumber;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCallbackStatus() {
        return callbackStatus;
    }

    public void setCallbackStatus(String callbackStatus) {
        this.callbackStatus = callbackStatus;
    }

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
    }

    public static class PaymentBuilder {
        private Long id;
        private Long orderId;
        private PaymentMethod method;
        private String reference;
        private BigDecimal amount;
        private PaymentStatus status;
        private LocalDateTime paidAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String merchantRequestId;
        private String checkoutRequestId;
        private String phoneNumber;
        private String mpesaReceiptNumber;
        private LocalDateTime transactionDate;
        private String callbackStatus;

        public PaymentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentBuilder method(PaymentMethod method) {
            this.method = method;
            return this;
        }

        public PaymentBuilder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public PaymentBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public PaymentBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PaymentBuilder merchantRequestId(String merchantRequestId) {
            this.merchantRequestId = merchantRequestId;
            return this;
        }

        public PaymentBuilder checkoutRequestId(String checkoutRequestId) {
            this.checkoutRequestId = checkoutRequestId;
            return this;
        }

        public PaymentBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public PaymentBuilder mpesaReceiptNumber(String mpesaReceiptNumber) {
            this.mpesaReceiptNumber = mpesaReceiptNumber;
            return this;
        }

        public PaymentBuilder transactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public PaymentBuilder callbackStatus(String callbackStatus) {
            this.callbackStatus = callbackStatus;
            return this;
        }

        public Payment build() {
            Payment payment = new Payment();
            payment.id = this.id;
            payment.orderId = this.orderId;
            payment.method = this.method;
            payment.reference = this.reference;
            payment.amount = this.amount;
            payment.status = this.status;
            payment.paidAt = this.paidAt;
            payment.createdAt = this.createdAt;
            payment.updatedAt = this.updatedAt;
            payment.merchantRequestId = this.merchantRequestId;
            payment.checkoutRequestId = this.checkoutRequestId;
            payment.phoneNumber = this.phoneNumber;
            payment.mpesaReceiptNumber = this.mpesaReceiptNumber;
            payment.transactionDate = this.transactionDate;
            payment.callbackStatus = this.callbackStatus;
            return payment;
        }
    }
}

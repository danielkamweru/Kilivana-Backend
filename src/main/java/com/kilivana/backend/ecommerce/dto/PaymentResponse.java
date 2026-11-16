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

    // Daraja (M-PESA STK Push) specific fields
    @Schema(description = "Daraja merchant request ID", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
    private String merchantRequestId;
    @Schema(description = "Daraja checkout request ID", example = "ws_CO_12345678901234567890")
    private String checkoutRequestId;
    @Schema(description = "Buyer's phone number", example = "254700000000")
    private String phoneNumber;
    @Schema(description = "M-PESA receipt number", example = "ABC123XYZ")
    private String mpesaReceiptNumber;
    @Schema(description = "M-PESA transaction date", example = "20261116093000")
    private LocalDateTime transactionDate;
    @Schema(description = "Callback processing status", example = "PROCESSED")
    private String callbackStatus;
    @Schema(description = "Daraja result code", example = "0")
    private Integer darajaResultCode;
    @Schema(description = "Daraja result description", example = "The service request is processed successfully")
    private String darajaResultDesc;

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

    public Integer getDarajaResultCode() {
        return darajaResultCode;
    }

    public void setDarajaResultCode(Integer darajaResultCode) {
        this.darajaResultCode = darajaResultCode;
    }

    public String getDarajaResultDesc() {
        return darajaResultDesc;
    }

    public void setDarajaResultDesc(String darajaResultDesc) {
        this.darajaResultDesc = darajaResultDesc;
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
        private String merchantRequestId;
        private String checkoutRequestId;
        private String phoneNumber;
        private String mpesaReceiptNumber;
        private LocalDateTime transactionDate;
        private String callbackStatus;
        private Integer darajaResultCode;
        private String darajaResultDesc;

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

        public PaymentResponseBuilder merchantRequestId(String merchantRequestId) {
            this.merchantRequestId = merchantRequestId;
            return this;
        }

        public PaymentResponseBuilder checkoutRequestId(String checkoutRequestId) {
            this.checkoutRequestId = checkoutRequestId;
            return this;
        }

        public PaymentResponseBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public PaymentResponseBuilder mpesaReceiptNumber(String mpesaReceiptNumber) {
            this.mpesaReceiptNumber = mpesaReceiptNumber;
            return this;
        }

        public PaymentResponseBuilder transactionDate(LocalDateTime transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public PaymentResponseBuilder callbackStatus(String callbackStatus) {
            this.callbackStatus = callbackStatus;
            return this;
        }

        public PaymentResponseBuilder darajaResultCode(Integer darajaResultCode) {
            this.darajaResultCode = darajaResultCode;
            return this;
        }

        public PaymentResponseBuilder darajaResultDesc(String darajaResultDesc) {
            this.darajaResultDesc = darajaResultDesc;
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
            paymentResponse.merchantRequestId = this.merchantRequestId;
            paymentResponse.checkoutRequestId = this.checkoutRequestId;
            paymentResponse.phoneNumber = this.phoneNumber;
            paymentResponse.mpesaReceiptNumber = this.mpesaReceiptNumber;
            paymentResponse.transactionDate = this.transactionDate;
            paymentResponse.callbackStatus = this.callbackStatus;
            paymentResponse.darajaResultCode = this.darajaResultCode;
            paymentResponse.darajaResultDesc = this.darajaResultDesc;
            return paymentResponse;
        }
    }
}

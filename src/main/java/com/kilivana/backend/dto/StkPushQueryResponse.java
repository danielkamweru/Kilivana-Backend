package com.kilivana.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response from Safaricom Daraja STK Push Query API.
 *
 * <p>Used to reconcile payments when the callback is lost or delayed.
 * A {@code ResultCode} of {@code 0} means the transaction completed
 * successfully; a non-zero code means it failed, was cancelled, or timed out.
 */
@Schema(description = "Response from Daraja STK Push Query API")
public class StkPushQueryResponse {

    @Schema(description = "Result code: 0 = success, non-zero = failure/cancel/timeout", example = "0")
    private Integer resultCode;

    @Schema(description = "Description of the result", example = "The service request is processed successfully.")
    private String resultDesc;

    @Schema(description = "Daraja merchant request ID", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
    private String merchantRequestID;

    @Schema(description = "Daraja checkout request ID", example = "ws_CO_12345678901234567890")
    private String checkoutRequestID;

    @Schema(description = "Daraja response code", example = "0")
    private String responseCode;

    @Schema(description = "Human-readable description of the response code", example = "Success")
    private String responseDescription;

    @Schema(description = "Message shown to the customer", example = "Please enter your M-PESA PIN")
    private String customerMessage;

    @Schema(description = "Transaction amount in KES (present only on success)", example = "100")
    private Long amount;

    @Schema(description = "M-PESA receipt number (present only on success)", example = "ABC123XYZ")
    private String mpesaReceiptNumber;

    @Schema(description = "Transaction date in yyyyMMddHHmmss format (present only on success)", example = "20261116093000")
    private String transactionDate;

    @Schema(description = "Buyer phone number (present only on success)", example = "254700000000")
    private String phoneNumber;

    public Integer getResultCode() {
        return resultCode;
    }

    public void setResultCode(Integer resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
    }

    public String getMerchantRequestID() {
        return merchantRequestID;
    }

    public void setMerchantRequestID(String merchantRequestID) {
        this.merchantRequestID = merchantRequestID;
    }

    public String getCheckoutRequestID() {
        return checkoutRequestID;
    }

    public void setCheckoutRequestID(String checkoutRequestID) {
        this.checkoutRequestID = checkoutRequestID;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public void setResponseDescription(String responseDescription) {
        this.responseDescription = responseDescription;
    }

    public String getCustomerMessage() {
        return customerMessage;
    }

    public void setCustomerMessage(String customerMessage) {
        this.customerMessage = customerMessage;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getMpesaReceiptNumber() {
        return mpesaReceiptNumber;
    }

    public void setMpesaReceiptNumber(String mpesaReceiptNumber) {
        this.mpesaReceiptNumber = mpesaReceiptNumber;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

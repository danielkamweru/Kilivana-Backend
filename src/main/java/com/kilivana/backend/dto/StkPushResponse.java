package com.kilivana.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response from Daraja after initiating an STK Push.
 *
 * <p>A {@code ResponseCode} of {@code "0"} means the request was accepted for
 * processing — the buyer will receive the STK prompt. It does NOT mean the
 * payment succeeded. Final outcome arrives via the callback endpoint.
 */
@Schema(description = "Response from Daraja after initiating an STK Push")
public class StkPushResponse {

    @Schema(description = "Unique ID from Daraja for this merchant request", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
    private String merchantRequestID;

    @Schema(description = "Unique ID for this STK checkout session, used to match the callback", example = "ws_CO_12345678901234567890")
    private String checkoutRequestID;

    @Schema(description = "Daraja response code: 0 = accepted for processing", example = "0")
    private String responseCode;

    @Schema(description = "Human-readable description of the response code", example = "Success. Request accepted for processing")
    private String responseDescription;

    @Schema(description = "Message shown to the customer on the STK prompt", example = "Please enter your M-PESA PIN to complete the transaction")
    private String customerMessage;

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
}
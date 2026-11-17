package com.kilivana.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Objects;

/**
 * Incoming callback payload from Safaricom Daraja after the buyer completes
 * (or cancels) the STK Push on their phone.
 *
 * <p>The JSON field names below match Safaricom's actual callback payload
 * exactly (see {@code https://developer.safaricom.co.ke/docs}):
 *
 * <pre>
 * {
 *   "Body": {
 *     "stkCallback": {
 *       "MerchantRequestID": "...",
 *       "CheckoutRequestID": "...",
 *       "ResultCode": 0,
 *       "ResultDesc": "...",
 *       "CallbackMetadata": {
 *         "Item": [
 *           {"Name": "Amount", "Value": 100},
 *           {"Name": "MpesaReceiptNumber", "Value": "ABC123XYZ"}
 *         ]
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Only the fields used for reconciliation are mapped; the full JSON is
 * logged (without sensitive data) for audit purposes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Callback payload from Safaricom Daraja after STK Push completion")
@Data
public class DarajaCallbackRequest {

    @JsonProperty("Body")
    @Schema(description = "Wrapper object containing the STK callback details")
    private BodyWrapper body;

    public StkCallback getBody() {
        return body != null ? body.getStkCallback() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Wrapper containing stkCallback")
    @Data
    public static class BodyWrapper {
        @JsonProperty("stkCallback")
        private StkCallback stkCallback;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "The actual STK callback details")
    @Data
    public static class StkCallback {

        @JsonProperty("MerchantRequestID")
        @Schema(description = "Daraja merchant request ID", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
        private String merchantRequestID;

        @JsonProperty("CheckoutRequestID")
        @Schema(description = "Daraja checkout request ID (matches the initiate response)", example = "ws_CO_12345678901234567890")
        private String checkoutRequestID;

        @JsonProperty("ResultCode")
        @Schema(description = "Result code: 0 = success, non-zero = failure/cancel/timeout", example = "0")
        private Integer resultCode;

        @JsonProperty("ResultDesc")
        @Schema(description = "Description of the result", example = "The service request is processed successfully.")
        private String resultDesc;

        @JsonProperty("CallbackMetadata")
        @Schema(description = "Callback metadata containing transaction details (present only on success)")
        private CallbackMetadata callbackMetadata;

        @Data
        public static class CallbackMetadata {

            @JsonProperty("Item")
            @Schema(description = "List of key-value pairs with transaction details")
            private List<Item> item;

            @Data
            public static class Item {
                @JsonProperty("Name")
                @Schema(description = "Name of the field", example = "Amount")
                private String name;

                @JsonProperty("Value")
                @Schema(description = "Value of the field", example = "100")
                private Object value;
            }
        }

        /**
         * Extracts the paid amount from metadata, or null if not present.
         */
        public Long getAmount() {
            if (callbackMetadata == null || callbackMetadata.getItem() == null) {
                return null;
            }
            return callbackMetadata.getItem().stream()
                    .filter(i -> "Amount".equalsIgnoreCase(i.getName()))
                    .map(CallbackMetadata.Item::getValue)
                    .filter(Objects::nonNull)
                    .map(v -> v instanceof Number n ? n.longValue()
                            : new java.math.BigDecimal(v.toString().trim()).longValue())
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Extracts the M-PESA receipt number from metadata, or null if not present.
         */
        public String getMpesaReceiptNumber() {
            if (callbackMetadata == null || callbackMetadata.getItem() == null) {
                return null;
            }
            return callbackMetadata.getItem().stream()
                    .filter(i -> "MpesaReceiptNumber".equalsIgnoreCase(i.getName()))
                    .map(CallbackMetadata.Item::getValue)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Extracts the transaction date from metadata, or null if not present.
         * Format: yyyyMMddHHmmss
         */
        public String getTransactionDate() {
            if (callbackMetadata == null || callbackMetadata.getItem() == null) {
                return null;
            }
            return callbackMetadata.getItem().stream()
                    .filter(i -> "TransactionDate".equalsIgnoreCase(i.getName()))
                    .map(CallbackMetadata.Item::getValue)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Extracts the phone number from metadata, or null if not present.
         */
        public String getPhoneNumber() {
            if (callbackMetadata == null || callbackMetadata.getItem() == null) {
                return null;
            }
            return callbackMetadata.getItem().stream()
                    .filter(i -> "PhoneNumber".equalsIgnoreCase(i.getName()))
                    .map(CallbackMetadata.Item::getValue)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .findFirst()
                    .orElse(null);
        }
    }
}

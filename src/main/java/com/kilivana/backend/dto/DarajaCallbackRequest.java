package com.kilivana.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Incoming callback payload from Safaricom Daraja after the buyer completes
 * (or cancels) the STK Push on their phone.
 *
 * <p>Only the fields used for reconciliation are mapped; the full JSON is
 * logged (without sensitive data) for audit purposes.
 */
@Schema(description = "Callback payload from Safaricom Daraja after STK Push completion")
@Data
public class DarajaCallbackRequest {

    @Schema(description = "Wrapper object containing the STK callback details")
    private StkCallback body;

    @Schema(description = "The actual STK callback details")
    @Data
    public static class StkCallback {

        @Schema(description = "Daraja merchant request ID", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
        private String merchantRequestID;

        @Schema(description = "Daraja checkout request ID (matches the initiate response)", example = "ws_CO_12345678901234567890")
        private String checkoutRequestID;

        @Schema(description = "Result code: 0 = success, non-zero = failure/cancel/timeout", example = "0")
        private int resultCode;

        @Schema(description = "Description of the result", example = "The service request is processed successfully.")
        private String resultDesc;

        @Schema(description = "Callback metadata containing transaction details (present only on success)")
        private CallbackMetadata callbackMetadata;

        @Data
        public static class CallbackMetadata {

            @Schema(description = "List of key-value pairs with transaction details")
            private java.util.List<Item> item;

            @Data
            public static class Item {
                @Schema(description = "Name of the field", example = "Amount")
                private String name;

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
                    .map(i -> ((Number) i.getValue()).longValue())
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
                    .map(i -> (String) i.getValue())
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
                    .map(i -> (String) i.getValue())
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
                    .map(i -> (String) i.getValue())
                    .findFirst()
                    .orElse(null);
        }
    }
}
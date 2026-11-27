package com.kilivana.backend.finance.service;

import com.kilivana.backend.config.DarajaProperties;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
import com.kilivana.backend.finance.enums.PayoutMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Safaricom M-Pesa B2C (Business to Customer) API integration for seller payouts.
 * Separate from STK Push - uses different endpoint and authentication flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaB2CService {

    private final DarajaProperties properties;
    private final WebClient webClient;

    /** Cached OAuth token with its expiry instant. */
    private final AtomicReference<TokenHolder> tokenCache = new AtomicReference<>();

    /** In-memory map for idempotency and reconciliation. */
    private final Map<String, B2CRequestRecord> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Initiates a B2C payment to a seller's M-Pesa account.
     *
     * @param phoneNumber   recipient's phone in 2547XXXXXXXX format
     * @param amount        amount in KES (will be converted to integer)
     * @param reference     transaction reference (max 12 chars for CommandID)
     * @param idempotencyKey unique key to prevent duplicate submissions
     * @return provider reference (ConversationID) for tracking
     */
    public String initiateB2CPayment(String phoneNumber, java.math.BigDecimal amount,
                                     String reference, String idempotencyKey) {
        // Check idempotency
        if (pendingRequests.containsKey(idempotencyKey)) {
            B2CRequestRecord existing = pendingRequests.get(idempotencyKey);
            log.warn("Duplicate B2C request with idempotency key {}: returning existing ConversationID {}",
                    idempotencyKey, existing.conversationID());
            return existing.conversationID();
        }

        String accessToken = getValidAccessToken();

        int amountInt = amount.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        if (amountInt <= 0) {
            throw new IllegalArgumentException("B2C amount must be positive");
        }
        if (amountInt > 70000) {
            throw new IllegalArgumentException("B2C amount exceeds M-Pesa limit of 70,000 KES");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String securityCredential = generateSecurityCredential();

        // CommandID: BusinessPayment (for salary/wages), SalaryPayment, PromotionPayment
        // BusinessPayment is standard for marketplace payouts
        String commandID = "BusinessPayment";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("InitiatorName", properties.getInitiatorName());
        requestBody.put("SecurityCredential", securityCredential);
        requestBody.put("CommandID", commandID);
        requestBody.put("Amount", amountInt);
        requestBody.put("PartyA", properties.getB2cShortcode());
        requestBody.put("PartyB", phoneNumber);
        requestBody.put("Remarks", "Kilivana payout: " + reference);
        requestBody.put("QueueTimeOutURL", properties.getB2cQueueTimeoutUrl());
        requestBody.put("ResultURL", properties.getB2cResultUrl());
        requestBody.put("Occasion", reference);

        log.info("Initiating B2C payment to phone {} for amount {} (reference={})",
                maskPhone(phoneNumber), amountInt, reference);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(properties.getB2cUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (response == null) {
                throw new ServiceUnavailableException("Daraja B2C returned empty response");
            }

            String conversationID = (String) response.get("ConversationID");
            String originatorConversationID = (String) response.get("OriginatorConversationID");
            String responseCode = (String) response.get("ResponseCode");
            String responseDescription = (String) response.get("ResponseDescription");

            if (!"0".equals(responseCode)) {
                String errorMsg = "B2C request failed: " + responseCode + " - " + responseDescription;
                log.error("Daraja B2C failed: {}", errorMsg);
                throw new ServiceUnavailableException(errorMsg);
            }

            // Record pending request for callback reconciliation
            if (conversationID != null) {
                B2CRequestRecord record = new B2CRequestRecord(
                        phoneNumber, amountInt, reference, commandID,
                        conversationID, originatorConversationID, LocalDateTime.now()
                );
                pendingRequests.put(idempotencyKey, record);
                // Also index by conversation ID for callback lookup
                pendingRequests.put("CONV-" + conversationID, record);
            }

            log.info("B2C payment accepted: ConversationID={}, OriginatorConversationID={}",
                    conversationID, originatorConversationID);
            return conversationID;

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.error("Daraja B2C failed with status {}: {}", e.getStatusCode(), body);
            throw new ServiceUnavailableException("M-Pesa B2C failed: " + body);
        } catch (Exception e) {
            log.error("Daraja B2C failed unexpectedly", e);
            throw new ServiceUnavailableException("M-Pesa B2C failed: " + e.getMessage());
        }
    }

    /**
     * Processes the B2C result callback from Safaricom.
     *
     * @param callback the parsed callback payload
     * @return true if processed successfully
     */
    public boolean processB2CCallback(Map<String, Object> callback) {
        Map<String, Object> result = (Map<String, Object>) callback.get("Result");
        if (result == null) {
            log.warn("B2C callback missing Result object");
            return false;
        }

        String conversationID = (String) result.get("ConversationID");
        String originatorConversationID = (String) result.get("OriginatorConversationID");
        Integer resultCode = parseResultCode(result.get("ResultCode"));
        String resultDesc = (String) result.get("ResultDesc");

        log.info("Processing B2C callback: ConversationID={}, ResultCode={}, ResultDesc={}",
                conversationID, resultCode, resultDesc);

        // Find the pending request
        B2CRequestRecord record = pendingRequests.remove("CONV-" + conversationID);
        if (record == null) {
            log.warn("No pending B2C request found for ConversationID={}", conversationID);
            // Still process - might be a delayed callback
        }

        // Extract transaction details from ResultParameters
        String mpesaReceiptNumber = null;
        String transactionAmount = null;
        String receiverPhone = null;

        Map<String, Object> resultParameters = (Map<String, Object>) result.get("ResultParameters");
        if (resultParameters != null) {
            var items = (java.util.List<Map<String, Object>>) resultParameters.get("ResultParameter");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    String key = (String) item.get("Key");
                    Object value = item.get("Value");
                    if ("MpesaReceiptNumber".equals(key)) {
                        mpesaReceiptNumber = String.valueOf(value);
                    } else if ("TransactionAmount".equals(key)) {
                        transactionAmount = String.valueOf(value);
                    } else if ("ReceiverPhoneNumber".equals(key)) {
                        receiverPhone = String.valueOf(value);
                    }
                }
            }
        }

        // Store result for the payout service to process
        if (record != null) {
            record = new B2CRequestRecord(
                    record.phoneNumber(), record.amount(), record.reference(), record.commandID(),
                    record.conversationID(), record.originatorConversationID(), record.initiatedAt(),
                    resultCode, resultDesc, mpesaReceiptNumber, transactionAmount, receiverPhone
            );
            pendingRequests.put("RESULT-" + conversationID, record);
        }

        return true;
    }

    /**
     * Processes the B2C timeout/queue callback.
     */
    public boolean processB2CQueueCallback(Map<String, Object> callback) {
        Map<String, Object> result = (Map<String, Object>) callback.get("Result");
        if (result == null) return false;

        String conversationID = (String) result.get("ConversationID");
        Integer resultCode = parseResultCode(result.get("ResultCode"));
        String resultDesc = (String) result.get("ResultDesc");

        log.info("Processing B2C queue callback: ConversationID={}, ResultCode={}, ResultDesc={}",
                conversationID, resultCode, resultDesc);

        // Store for reconciliation
        B2CRequestRecord record = pendingRequests.remove("CONV-" + conversationID);
        if (record != null) {
            record = new B2CRequestRecord(
                    record.phoneNumber(), record.amount(), record.reference(), record.commandID(),
                    record.conversationID(), record.originatorConversationID(), record.initiatedAt(),
                    resultCode, resultDesc, null, null, null
            );
            pendingRequests.put("RESULT-" + conversationID, record);
        }

        return true;
    }

    /**
     * Retrieves the B2C result for a conversation ID (for reconciliation).
     */
    public B2CRequestRecord getB2CResult(String conversationID) {
        return pendingRequests.remove("RESULT-" + conversationID);
    }

    private String getValidAccessToken() {
        TokenHolder holder = tokenCache.get();
        if (holder != null && !holder.isExpired()) {
            return holder.token();
        }
        return fetchAndCacheNewToken();
    }

    private String fetchAndCacheNewToken() {
        if (properties.getConsumerKey() == null || properties.getConsumerKey().isBlank()
                || properties.getConsumerSecret() == null || properties.getConsumerSecret().isBlank()) {
            throw new ServiceUnavailableException(
                    "Failed to obtain M-Pesa access token: DARAJA_CONSUMER_KEY/SECRET not configured");
        }

        String credentials = properties.getConsumerKey() + ":" + properties.getConsumerSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        log.debug("Fetching new Daraja OAuth token for B2C");

        try {
            Map<String, Object> response = webClient.get()
                    .uri(properties.getOAuthUrl())
                    .header("Authorization", "Basic " + encoded)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (response == null || response.get("access_token") == null) {
                throw new ServiceUnavailableException("Daraja OAuth returned no access token");
            }

            String token = (String) response.get("access_token");
            int expiresIn = parseExpiresIn(response.get("expires_in"));
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(Math.max(60, expiresIn - 300));

            tokenCache.set(new TokenHolder(token, expiresAt));
            log.debug("Cached new Daraja OAuth token for B2C, expires at {}", expiresAt);
            return token;

        } catch (WebClientResponseException e) {
            log.error("Daraja OAuth failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceUnavailableException("Failed to obtain M-Pesa access token: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Daraja OAuth failed unexpectedly", e);
            throw new ServiceUnavailableException("Failed to obtain M-Pesa access token: " + e.getMessage());
        }
    }

    private int parseExpiresIn(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 3600;
    }

    private String generateSecurityCredential() {
        // SecurityCredential is Base64 encrypted Initiator Password using Safaricom's public key
        // For production, this should use the certificate from Safaricom
        // For sandbox, we can use a simple approach or the provided credential
        if (properties.getSecurityCredential() != null && !properties.getSecurityCredential().isBlank()) {
            return properties.getSecurityCredential();
        }

        // Fallback: In sandbox, some configurations use the initiator password directly encoded
        // This is NOT secure for production - production MUST use the encrypted credential
        if (properties.getInitiatorPassword() != null && !properties.getInitiatorPassword().isBlank()) {
            log.warn("Using InitiatorPassword as SecurityCredential - NOT SECURE FOR PRODUCTION");
            return Base64.getEncoder().encodeToString(properties.getInitiatorPassword().getBytes());
        }

        throw new ServiceUnavailableException("B2C SecurityCredential not configured. Set DARAJA_SECURITY_CREDENTIAL or DARAJA_INITIATOR_PASSWORD");
    }

    private int parseResultCode(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "****";
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 2);
    }

    /** Simple holder for a cached token and its expiry. */
    private record TokenHolder(String token, LocalDateTime expiresAt) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /** Metadata recorded when a B2C payment is initiated. */
    public record B2CRequestRecord(
            String phoneNumber,
            int amount,
            String reference,
            String commandID,
            String conversationID,
            String originatorConversationID,
            LocalDateTime initiatedAt,
            Integer resultCode,
            String resultDesc,
            String mpesaReceiptNumber,
            String transactionAmount,
            String receiverPhone
    ) {
        public B2CRequestRecord(String phoneNumber, int amount, String reference, String commandID,
                                String conversationID, String originatorConversationID, LocalDateTime initiatedAt) {
            this(phoneNumber, amount, reference, commandID, conversationID, originatorConversationID, initiatedAt,
                    null, null, null, null, null);
        }
    }
}
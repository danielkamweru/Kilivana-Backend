package com.kilivana.backend.service;

import com.kilivana.backend.config.DarajaProperties;
import com.kilivana.backend.dto.StkPushResponse;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
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
 * Safaricom Daraja (M-PESA Express / STK Push) client.
 *
 * <p>Handles OAuth access-token acquisition with caching and expiry, STK password
 * generation, and STK Push request/response processing. All credentials are
 * loaded from {@link DarajaProperties} and are never logged.
 *
 * <p>Token caching uses a simple in-memory holder with a validity window slightly
 * shorter than the token's 1-hour TTL to avoid using an expired token.
 *
 * <p>The STK password is generated per the Daraja specification:
 * {@code Base64(Shortcode + Passkey + Timestamp)} where the timestamp uses
 * {@code yyyyMMddHHmmss} format in the system default timezone.
 */
@Service
public class DarajaService {
    private final DarajaProperties properties;
    private final WebClient webClient;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DarajaService.class);

    public DarajaService(DarajaProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    /** Cached OAuth token with its expiry instant. */
    private final AtomicReference<TokenHolder> tokenCache = new AtomicReference<>();

    /** In-memory map of CheckoutRequestID -> STK request metadata for reconciliation. */
    private final Map<String, StkRequestRecord> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Initiates an STK Push to the buyer's phone.
     *
     * @param phoneNumber   buyer's phone in 2547XXXXXXXX format (normalized by caller)
     * @param amount        integer amount in KES (caller converts from BigDecimal)
     * @param accountRef    account reference (max 12 chars, used by buyer to identify)
     * @param transactionDesc description shown on the STK prompt (max 13 chars)
     * @return STK push response containing CheckoutRequestID and MerchantRequestID
     */
    public StkPushResponse initiateStkPush(String phoneNumber, int amount,
                                           String accountRef, String transactionDesc) {
        String accessToken = getValidAccessToken();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password = generateStkPassword(timestamp);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("BusinessShortCode", properties.getShortcode());
        requestBody.put("Password", password);
        requestBody.put("Timestamp", timestamp);
        requestBody.put("TransactionType", "CustomerPayBillOnline");
        requestBody.put("Amount", amount);
        requestBody.put("PartyA", phoneNumber);
        requestBody.put("PartyB", properties.getShortcode());
        requestBody.put("PhoneNumber", phoneNumber);
        requestBody.put("CallBackURL", properties.getCallbackUrl());
        requestBody.put("AccountReference", accountRef);
        requestBody.put("TransactionDesc", transactionDesc);

        log.info("Initiating STK Push to phone {} for amount {} (accountRef={})",
                maskPhone(phoneNumber), amount, accountRef);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(properties.getStkPushUrl())
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (response == null) {
                throw new ServiceUnavailableException("Daraja returned empty response");
            }

            StkPushResponse stkResponse = parseStkResponse(response);

            // Record the pending request for callback reconciliation
            if (stkResponse.getCheckoutRequestID() != null) {
                pendingRequests.put(stkResponse.getCheckoutRequestID(),
                        new StkRequestRecord(phoneNumber, amount, accountRef, transactionDesc,
                                stkResponse.getMerchantRequestID(), LocalDateTime.now()));
            }

            log.info("STK Push accepted: MerchantRequestID={}, CheckoutRequestID={}",
                    stkResponse.getMerchantRequestID(), stkResponse.getCheckoutRequestID());
            return stkResponse;

        } catch (WebClientResponseException e) {
            log.error("Daraja STK Push failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceUnavailableException("M-PESA STK Push failed: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Daraja STK Push failed unexpectedly", e);
            throw new ServiceUnavailableException("M-PESA STK Push failed: " + e.getMessage());
        }
    }

    /**
     * Retrieves a valid OAuth access token, fetching a new one if the cached token
     * is missing or within 5 minutes of expiry.
     * Package-private for testing.
     */
    String getValidAccessToken() {
        TokenHolder holder = tokenCache.get();
        if (holder != null && !holder.isExpired()) {
            return holder.token();
        }
        return fetchAndCacheNewToken();
    }

    /** Obtains a fresh OAuth token from Daraja and caches it. */
    private String fetchAndCacheNewToken() {
        if (properties.getConsumerKey() == null || properties.getConsumerKey().isBlank()
                || properties.getConsumerSecret() == null || properties.getConsumerSecret().isBlank()) {
            throw new ServiceUnavailableException(
                    "Failed to obtain M-PESA access token: DARAJA_CONSUMER_KEY/SECRET not configured");
        }

        String credentials = properties.getConsumerKey() + ":" + properties.getConsumerSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        log.debug("Fetching new Daraja OAuth token");

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
            // Use 5 minutes before actual expiry as safety margin
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(Math.max(60, expiresIn - 300));

            tokenCache.set(new TokenHolder(token, expiresAt));
            log.debug("Cached new Daraja OAuth token, expires at {}", expiresAt);
            return token;

        } catch (WebClientResponseException e) {
            log.error("Daraja OAuth failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ServiceUnavailableException("Failed to obtain M-PESA access token: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Daraja OAuth failed unexpectedly", e);
            throw new ServiceUnavailableException("Failed to obtain M-PESA access token: " + e.getMessage());
        }
    }

    /**
     * Reads the token lifetime from the OAuth response. Safaricom sends it as a JSON
     * string ({@code "3599"}), so casting the field to a number fails on the real
     * response and takes the whole STK Push down with it. A number is accepted too,
     * and anything unreadable falls back to the documented hour.
     */
    private int parseExpiresIn(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
                // Unreadable lifetime: fall back to the documented hour.
            }
        }
        return 3600;
    }

    /** Generates the STK password: Base64(Shortcode + Passkey + Timestamp). */
    private String generateStkPassword(String timestamp) {
        String raw = properties.getShortcode() + properties.getPasskey() + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes());
    }

    private StkPushResponse parseStkResponse(Map<String, Object> response) {
        StkPushResponse result = new StkPushResponse();
        result.setMerchantRequestID((String) response.get("MerchantRequestID"));
        result.setCheckoutRequestID((String) response.get("CheckoutRequestID"));
        result.setResponseCode((String) response.get("ResponseCode"));
        result.setResponseDescription((String) response.get("ResponseDescription"));
        result.setCustomerMessage((String) response.get("CustomerMessage"));
        return result;
    }

    /** Masks a phone number for logging (keeps first 3 and last 2 digits). */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "****";
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 2);
    }

    /**
     * Retrieves and removes the pending request record for a CheckoutRequestID.
     * Returns null if no record exists (e.g., callback for unknown request).
     */
    public StkRequestRecord consumePendingRequest(String checkoutRequestID) {
        return pendingRequests.remove(checkoutRequestID);
    }

    /** Simple holder for a cached token and its expiry. */
    private record TokenHolder(String token, LocalDateTime expiresAt) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /** Metadata recorded when an STK Push is initiated, for callback reconciliation. */
    public record StkRequestRecord(
            String phoneNumber,
            int amount,
            String accountReference,
            String transactionDesc,
            String merchantRequestID,
            LocalDateTime initiatedAt
    ) {}
}
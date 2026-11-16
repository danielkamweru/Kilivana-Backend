package com.kilivana.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Safaricom Daraja (M-PESA Express / STK Push) configuration.
 * All credentials are sourced from environment variables — never committed to source.
 * The sandbox environment is the default; switch to "production" only after
 * passing Safaricom's go-live checks.
 */
@Component
@ConfigurationProperties(prefix = "app.daraja")
public class DarajaProperties {

    private static final Logger log = LoggerFactory.getLogger(DarajaProperties.class);

    /**
     * Consumer key from the Daraja portal application "Kilivana-Backend".
     * Used to obtain the OAuth access token.
     */
    private String consumerKey;

    /**
     * Consumer secret from the Daraja portal application "Kilivana-Backend".
     * Used to obtain the OAuth access token.
     */
    private String consumerSecret;

    /**
     * Business shortcode (PayBill or Till number).
     * Sandbox default: 174379. Replace with production shortcode on go-live.
     */
    private String shortcode = "174379";

    /**
     * Base64-encoded passkey from the Daraja portal (sandbox or production).
     * Used to generate the STK password: Base64(Shortcode + Passkey + Timestamp).
     */
    private String passkey;

    /**
     * Public HTTPS callback URL where Safaricom posts the STK result.
     * Must be reachable by Safaricom (use ngrok or similar for local testing).
     */
    private String callbackUrl;

    private String callbackPath = "/api/v1/payments/daraja/callback";

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    /**
     * Daraja environment: "sandbox" or "production".
     * Determines the base URL for OAuth and STK Push endpoints.
     */
    private String environment = "sandbox";

    /**
     * HTTP timeout (seconds) for Daraja HTTP calls.
     * Prevents hanging threads on slow or unresponsive Safaricom endpoints.
     */
    private int timeoutSeconds = 30;

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(String shortcode) {
        this.shortcode = shortcode;
    }

    public String getPasskey() {
        return passkey;
    }

    public void setPasskey(String passkey) {
        this.passkey = passkey;
    }

    public String getCallbackUrl() {
        if (callbackUrl != null && !callbackUrl.isBlank()) return callbackUrl;
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) return callbackPath;
        return publicBaseUrl.replaceAll("\\/+$", "") + callbackPath;
    }

    /**
     * Validates the callback URL configuration on startup. Logs a warning when
     * the callback falls back to a relative path, which Safaricom cannot reach.
     * Deployments must set {@code DARAJA_CALLBACK_URL} or
     * {@code PUBLIC_BACKEND_URL} to a public HTTPS endpoint.
     */
    @PostConstruct
    public void validateCallbackUrl() {
        String resolved = getCallbackUrl();
        if (resolved.equals(callbackPath)) {
            log.warn("""
                Daraja callback URL is not configured as a public HTTPS endpoint.
                Safaricom cannot reach a relative path ({}). Set DARAJA_CALLBACK_URL
                or PUBLIC_BACKEND_URL to a publicly accessible HTTPS URL (use ngrok
                for local development). Otherwise STK Push callbacks will fail and
                payments will remain PENDING.""", resolved);
        } else {
            log.info("Daraja callback URL configured: {}", resolved);
        }
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getCallbackPath() { return callbackPath; }
    public void setCallbackPath(String callbackPath) { this.callbackPath = callbackPath; }
    void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Returns the OAuth token endpoint for the configured environment. */
    public String getOAuthUrl() {
        if ("production".equalsIgnoreCase(environment)) {
            return "https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
        }
        return "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
    }

    /** Returns the STK Push endpoint for the configured environment. */
    public String getStkPushUrl() {
        if ("production".equalsIgnoreCase(environment)) {
            return "https://api.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
        }
        return "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";
    }

    /** Returns the STK Push Query endpoint for the configured environment. */
    public String getStkPushQueryUrl() {
        if ("production".equalsIgnoreCase(environment)) {
            return "https://api.safaricom.co.ke/mpesa/stkpushquery/v1/query";
        }
        return "https://sandbox.safaricom.co.ke/mpesa/stkpushquery/v1/query";
    }
}
package com.kilivana.backend.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class SendGridEventWebhookService {
    private final SendGridProperties properties;
    private final ObjectMapper objectMapper;

    public SendGridEventWebhookService(SendGridProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void process(String payload, String signature, String timestamp) {
        String publicKey = properties.getEventWebhookPublicKey();
        if (publicKey == null || publicKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SendGrid event webhook signature verification is not configured");
        }
        if (payload == null || signature == null || signature.isBlank()
                || timestamp == null || timestamp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SendGrid event signature");
        }

        try {
            byte[] encodedKey = Base64.getDecoder().decode(publicKey.trim());
            ECPublicKey key = (ECPublicKey) KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(encodedKey));
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key);
            verifier.update(timestamp.getBytes(StandardCharsets.UTF_8));
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SendGrid event signature");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SendGrid event signature");
        }
        JsonNode events;
        try {
            events = objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SendGrid event payload");
        }
        if (events == null || !events.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SendGrid event payload");
        }

        for (JsonNode event : events) {
            if (!event.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SendGrid event payload");
            }
            logOutcome(event);
        }
    }

    private void logOutcome(JsonNode event) {
        String eventName = text(event, "event");
        String outcome = switch (eventName.toLowerCase(java.util.Locale.ROOT)) {
            case "delivered" -> "DELIVERED";
            case "deferred" -> "DEFERRED";
            case "bounce" -> "BOUNCED";
            case "dropped", "blocked", "failed" -> "FAILED";
            case "processed" -> "ACCEPTED";
            default -> null;
        };
        if (outcome == null) {
            log.debug("Ignoring SendGrid event type={}", safe(eventName));
            return;
        }

        String recipient = text(event, "email");
        String purpose = customArgument(event, "kilivana_purpose");
        String challengeId = customArgument(event, "kilivana_challenge_id");
        String smtpStatus = text(event, "status");
        String response = text(event, "response");
        if ("DELIVERED".equals(outcome)) {
            log.info("SendGrid delivery outcome={} recipient={} purpose={} challengeId={} smtpStatus={} response={}",
                    outcome, safe(recipient), safe(purpose), safe(challengeId), safe(smtpStatus), safe(response));
        } else if ("DEFERRED".equals(outcome)) {
            log.warn("SendGrid delivery outcome={} recipient={} purpose={} challengeId={} smtpStatus={} response={}",
                    outcome, safe(recipient), safe(purpose), safe(challengeId), safe(smtpStatus), safe(response));
        } else if ("ACCEPTED".equals(outcome)) {
            log.info("SendGrid delivery outcome={} recipient={} purpose={} challengeId={} smtpStatus={} response={}",
                    outcome, safe(recipient), safe(purpose), safe(challengeId), safe(smtpStatus), safe(response));
        } else {
            log.error("SendGrid delivery outcome={} recipient={} purpose={} challengeId={} smtpStatus={} response={}",
                    outcome, safe(recipient), safe(purpose), safe(challengeId), safe(smtpStatus), safe(response));
        }
    }

    private String customArgument(JsonNode event, String key) {
        String value = text(event, key);
        if (value.isBlank()) {
            JsonNode customArgs = event.get("custom_args");
            if (customArgs != null && customArgs.isObject()) {
                value = text(customArgs, key);
            }
        }
        return value;
    }

    private String text(JsonNode event, String field) {
        JsonNode value = event.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String flattened = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return flattened.length() > 500 ? flattened.substring(0, 500) + "..." : flattened;
    }
}

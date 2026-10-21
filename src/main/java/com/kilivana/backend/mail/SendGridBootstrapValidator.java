package com.kilivana.backend.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Fails startup when SendGrid is switched on but cannot possibly work.
 *
 * <p>The mail service degrades gracefully by design: with the integration off
 * or the key absent it logs and returns {@code false}. That is the right shape
 * for a feature that is optional in development. What it must not do is start
 * up quietly with {@code SENDGRID_ENABLED=true} and a missing key, because a
 * deployment that thinks mail is working will silently deliver nothing.
 *
 * <p>This runner checks the configuration once, at bootstrap, and throws so the
 * container exits non-zero instead of running misconfigured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendGridBootstrapValidator implements ApplicationRunner {

    private final SendGridProperties properties;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.debug("SendGrid is disabled; skipping bootstrap validation");
            return;
        }

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "SendGrid is enabled (app.sendgrid.enabled=true) but SENDGRID_API_KEY is not set. "
                            + "Set the key or disable the integration.");
        }

        if (properties.getFromEmail() == null || properties.getFromEmail().isBlank()) {
            throw new IllegalStateException(
                    "SendGrid is enabled but SENDGRID_FROM_EMAIL is not set. "
                            + "The sender address must be verified in your SendGrid account.");
        }

        log.info("SendGrid bootstrap validated: enabled, from={}", mask(properties.getFromEmail()));
    }

    /** Logs a sender address without repeating the full value. */
    private String mask(String email) {
        if (email == null || email.length() <= 4) {
            return "****";
        }
        int at = email.indexOf('@');
        if (at < 0) {
            return email.substring(0, 2) + "***";
        }
        String local = email.substring(0, Math.max(1, at));
        return local.substring(0, Math.min(local.length(), 2)) + "***" + email.substring(at);
    }
}
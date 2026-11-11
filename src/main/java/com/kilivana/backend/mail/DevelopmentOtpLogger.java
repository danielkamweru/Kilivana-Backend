package com.kilivana.backend.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@Profile("dev & !prod & !production")
@ConditionalOnProperty(name = "app.email-verification.log-otp", havingValue = "true")
public class DevelopmentOtpLogger {
    public void log(String recipient, String purpose, Instant expiresAt, String code) {
        log.info("LOCAL DEVELOPMENT OTP recipient={} purpose={} expiresAt={} code={}",
                recipient, purpose, expiresAt, code);
    }
}

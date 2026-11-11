package com.kilivana.backend.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Slf4j
@Component
@Profile("dev & !prod & !production")
@ConditionalOnProperty(name = "app.email-verification.log-otp", havingValue = "true")
public class DevelopmentOtpLogger {
    public static final String REQUEST_ATTRIBUTE = DevelopmentOtpLogger.class.getName() + ".otp";

    public void log(String recipient, String purpose, Instant expiresAt, String code) {
        String details = "LOCAL DEVELOPMENT OTP recipient=" + recipient
                + " purpose=" + purpose
                + " expiresAt=" + expiresAt
                + " code=" + code;
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            servlet.getRequest().setAttribute(REQUEST_ATTRIBUTE, details);
            return;
        }
        log.info("HTTP 202 (OTP request accepted; asynchronous dispatch) | {}", details);
    }
}

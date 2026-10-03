package com.kilivana.backend.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renders the driver delivery-code email. Kept apart from SendGridMailService so the
 * logistics domain can describe the message without knowing how it is delivered.
 *
 * Inline styling and tables only: mail clients strip a stylesheet block and many of them
 * ignore anything beyond the first media query, so anything fancier arrives unreadable.
 */
@Component
public class DriverOtpEmailTemplate {

    private static final String SUBJECT = "Your Kilivana delivery code";

    private final String body;

    public DriverOtpEmailTemplate(@Value("classpath:email/driver-delivery-otp.html") String body) {
        this.body = body;
    }

    public String subject() {
        return SUBJECT;
    }

    /**
     * A blank the template cannot silently leave behind: a message with a literal
     * {@code {{otp}}} in it would read as a real code, so an empty value is refused.
     */
    public String render(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Delivery code email needs its values");
        }
        if (values.get("otp") == null || values.get("otp").isBlank()) {
            throw new IllegalArgumentException("Delivery code email needs a delivery code");
        }

        String html = body;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() == null ? "" : entry.getValue());
        }
        return html;
    }
}
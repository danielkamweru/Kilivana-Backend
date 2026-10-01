package com.kilivana.backend.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.sendgrid")
public class SendGridProperties {

    /** Off by default so a missing key cannot turn every failing request into a 500. */
    private boolean enabled = false;

    private String apiKey = "";

    private String fromEmail = "no-reply@kilivana.com";

    private String fromName = "Kilivana";

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
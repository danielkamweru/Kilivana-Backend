package com.kilivana.backend.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SendGrid connection settings, bound from {@code app.sendgrid.*}.
 *
 * <p>Externalized so the API key and sender identity come from the environment and are
 * never committed. Mail is off unless {@code enabled} is set, so a deployment without a
 * key degrades to skipping email rather than failing every request.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.sendgrid")
public class SendGridProperties {

    /** Off by default so a missing key cannot turn every failing request into a 500. */
    private boolean enabled = false;

    /** SendGrid API key, injected from the environment. */
    private String apiKey = "";

    private String fromEmail = "no-reply@kilivana.com";

    private String fromName = "Kilivana";

    /** Mail can be sent only when the integration is switched on and a key is present. */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
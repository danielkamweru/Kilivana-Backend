package com.kilivana.backend.mail;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Function;

/**
 * Sends transactional email through SendGrid.
 *
 * <p>A thin wrapper over the SendGrid SDK: it builds the message, posts it to the
 * {@code mail/send} endpoint and reports the outcome as a boolean, so callers can
 * treat a mail outage as a non-failure.
 *
 * <p>The {@link SendGrid} instance is produced by a factory rather than constructed
 * inline so the failure paths are testable without a live network call. The default
 * factory is {@code SendGrid::new}; tests substitute one that throws.
 */
@Slf4j
@Service
public class SendGridMailService {

    private final SendGridProperties properties;
    private final Function<String, SendGrid> sendGridFactory;

    
    @Autowired
    public SendGridMailService(SendGridProperties properties) {
        this(properties, SendGrid::new);
    }

    public SendGridMailService(SendGridProperties properties, Function<String, SendGrid> sendGridFactory) {
        this.properties = properties;
        this.sendGridFactory = sendGridFactory;
    }

    /**
     * Sends a single plain-text message. Delivery problems are logged rather than thrown:
     * an unreachable mail provider must not fail the business operation that triggered the
     * email. Callers that need to know should check the returned boolean.
     */
    public boolean send(String to, String subject, String body) {
        return dispatch(to, subject, "text/plain", body);
    }

    /** Sends an HTML message. */
    public boolean sendHtml(String to, String subject, String htmlBody) {
        return dispatch(to, subject, "text/html", htmlBody);
    }

    /**
     * Posts the message to SendGrid. Returns {@code false} when the integration is
     * unconfigured or the API call fails; never throws, so a mail outage cannot fail
     * the business operation that triggered the email.
     */
    private boolean dispatch(String to, String subject, String contentType, String content) {
        if (!properties.isConfigured()) {
            log.warn("SendGrid is not configured; skipping transactional email");
            return false;
        }

        Mail mail = new Mail(
                new Email(properties.getFromEmail(), properties.getFromName()),
                subject,
                new Email(to),
                new Content(contentType, content));

        try {
            // Built per send: keeps the service stateless and always uses the current key.
            SendGrid sendGrid = sendGridFactory.apply(properties.getApiKey());
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            com.sendgrid.Response response = sendGrid.api(request);
            if (response == null || response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                log.warn("Transactional email provider rejected delivery");
                return false;
            }
            log.info("Transactional email accepted by provider");
            return true;
        } catch (IOException | RuntimeException ex) {
            log.warn("Transactional email delivery failed");
            return false;
        }
    }
}
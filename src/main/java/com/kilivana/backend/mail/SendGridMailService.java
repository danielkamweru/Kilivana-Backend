package com.kilivana.backend.mail;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Sends transactional email through SendGrid.
 *
 * <p>A thin wrapper over the SendGrid SDK: it builds the message, posts it to the
 * {@code mail/send} endpoint and reports the outcome as a boolean, so callers can
 * treat a mail outage as a non-failure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendGridMailService {

    private final SendGridProperties properties;

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
            log.warn("SendGrid is not configured; skipping email to {} with subject '{}'", to, subject);
            return false;
        }

        Mail mail = new Mail(
                new Email(properties.getFromEmail(), properties.getFromName()),
                subject,
                new Email(to),
                new Content(contentType, content));

        try {
            // Built per send: keeps the service stateless and always uses the current key.
            SendGrid sendGrid = new SendGrid(properties.getApiKey());
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            sendGrid.api(request);
            log.info("Queued SendGrid {} message to {} with subject '{}'", contentType, to, subject);
            return true;
        } catch (IOException e) {
            log.error("Failed to send SendGrid {} message to {} with subject '{}'",
                    contentType, to, subject, e);
            return false;
        }
    }
}
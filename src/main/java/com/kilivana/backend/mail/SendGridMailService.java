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

    public boolean sendHtml(String to, String subject, String htmlBody) {
        return dispatch(to, subject, "text/html", htmlBody);
    }

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
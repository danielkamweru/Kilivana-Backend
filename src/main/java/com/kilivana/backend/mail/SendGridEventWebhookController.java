package com.kilivana.backend.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/sendgrid")
@RequiredArgsConstructor
public class SendGridEventWebhookController {
    private final SendGridEventWebhookService webhookService;

    @PostMapping("/events")
    public ResponseEntity<Void> receiveEvents(
            @RequestBody String payload,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Twilio-Email-Event-Webhook-Timestamp", required = false) String timestamp) {
        webhookService.process(payload, signature, timestamp);
        return ResponseEntity.ok().build();
    }
}

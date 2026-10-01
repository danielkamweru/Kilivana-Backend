package com.kilivana.backend.mail;

import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Administration · Email", description = "SendGrid integration and delivery checks")
@RestController
@RequestMapping("/api/v1/admin/mail")
@RequiredArgsConstructor
public class MailAdminController {

    private final SendGridMailService mailService;
    private final SendGridProperties properties;

    public record TestEmailRequest(String to, String subject, String body) {
    }

    /**
     * Sends a real message so an operator can confirm the SendGrid key, verified sender and
     * DNS records are all working. Admin-only: it sends mail to an address the caller chooses.
     */
    @PostMapping("/test")
    @Operation(summary = "Send a test email through SendGrid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTestEmail(
            @RequestBody TestEmailRequest request) {
        if (request.to() == null || request.to().isBlank()) {
            throw new BadRequestException("A recipient address is required");
        }

        if (!properties.isConfigured()) {
            return ResponseEntity.ok(ApiResponse.success(
                    "SendGrid is disabled or SENDGRID_API_KEY is not set. "
                    + "Set SENDGRID_ENABLED=true and restart.",
                    Map.of("configured", false, "sent", false)));
        }

        String subject = request.subject() == null ? "Kilivana test email" : request.subject();
        String body = request.body() == null
                ? "This is a test message from the Kilivana backend. If you can read it, "
                  + "SendGrid is configured correctly."
                : request.body();

        boolean sent = mailService.send(request.to(), subject, body);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", true);
        result.put("sent", sent);
        result.put("from", properties.getFromEmail());
        result.put("to", request.to());
        result.put("subject", subject);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
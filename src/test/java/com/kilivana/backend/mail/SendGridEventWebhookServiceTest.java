package com.kilivana.backend.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SendGridEventWebhookServiceTest {
    @Test
    void signedEventsDistinguishDeliveryOutcomesAndLogSmtpResponses() throws Exception {
        KeyPair pair = keyPair();
        SendGridProperties properties = properties(pair);
        SendGridEventWebhookService service = new SendGridEventWebhookService(properties, new ObjectMapper());
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(SendGridEventWebhookService.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try {
            String payload = """
                    [
                      {"event":"delivered","email":"a@example.test","status":"2.0.0","response":"250 2.0.0 OK","kilivana_purpose":"EMAIL_VERIFICATION"},
                      {"event":"deferred","email":"b@example.test","status":"4.7.32","response":"421 4.7.32 From domain alignment","custom_args":{"kilivana_purpose":"EMAIL_VERIFICATION"}},
                      {"event":"bounce","email":"c@example.test","status":"5.1.1","response":"550 5.1.1 No such user","kilivana_purpose":"EMAIL_VERIFICATION"},
                      {"event":"dropped","email":"d@example.test","status":"5.0.0","response":"Sender rejected","kilivana_purpose":"EMAIL_VERIFICATION"}
                    ]
                    """;
            String timestamp = "1794423597";
            service.process(payload, sign(pair, timestamp, payload), timestamp);

            assertThat(appender.list).anySatisfy(e -> assertThat(e.getFormattedMessage())
                    .contains("outcome=DELIVERED", "250 2.0.0 OK"));
            assertThat(appender.list).anySatisfy(e -> assertThat(e.getFormattedMessage())
                    .contains("outcome=DEFERRED", "421 4.7.32"));
            assertThat(appender.list).anySatisfy(e -> assertThat(e.getFormattedMessage())
                    .contains("outcome=BOUNCED", "550 5.1.1"));
            assertThat(appender.list).anySatisfy(e -> assertThat(e.getFormattedMessage())
                    .contains("outcome=FAILED", "Sender rejected"));
        } finally {logger.detachAppender(appender);appender.stop();}
    }

    @Test
    void rejectsUnsignedOrInvalidEvents() throws Exception {
        KeyPair pair = keyPair();
        SendGridEventWebhookService service = new SendGridEventWebhookService(properties(pair), new ObjectMapper());
        String timestamp = "1794423597";
        assertThat(assertThrows(ResponseStatusException.class,
                () -> service.process("[]", "not-a-valid-signature", timestamp)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(assertThrows(ResponseStatusException.class,
                () -> service.process("not-json", sign(pair, timestamp, "not-json"), timestamp)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        SendGridProperties unconfigured = new SendGridProperties();
        assertThat(assertThrows(ResponseStatusException.class,
                () -> new SendGridEventWebhookService(unconfigured, new ObjectMapper())
                        .process("[]", "signature", timestamp)).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }

    private SendGridProperties properties(KeyPair pair) {
        SendGridProperties properties = new SendGridProperties();
        properties.setEventWebhookPublicKey(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        return properties;
    }

    private String sign(KeyPair pair, String timestamp, String payload) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(pair.getPrivate());
        signer.update(timestamp.getBytes(StandardCharsets.UTF_8));
        signer.update(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }
}

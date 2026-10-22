package com.kilivana.backend.mail;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives {@link SendGridMailService} through the paths a caller can actually hit:
 * unconfigured, configured-and-successful and configured-but-provider-failure.
 *
 * <p>The SendGrid client is replaced with a subclass rather than a network mock,
 * so the checked {@link IOException} path is genuinely exercised.
 */
class SendGridMailServiceTest {

    private SendGridMailService service(boolean enabled, String apiKey) {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(enabled);
        properties.setApiKey(apiKey);
        properties.setFromEmail("no-reply@kilivana.com");
        properties.setFromName("Kilivana");
        return new SendGridMailService(properties, key -> new FailingSendGrid(key));
    }

    @Test
    void unconfigured_returnsFalseAndDoesNotCallProvider() {
        SendGridMailService service = service(false, "");
        assertThat(service.send("buyer@example.com", "Hi", "body")).isFalse();
        assertThat(service.sendHtml("buyer@example.com", "Hi", "<b>body</b>")).isFalse();
    }

    @Test
    void enabledButBlankKey_isTreatedAsUnconfigured() {
        SendGridMailService service = service(true, "   ");
        // isConfigured() requires a non-blank key, so this must not reach the provider.
        assertThat(service.send("buyer@example.com", "Hi", "body")).isFalse();
    }

    @Test
    void providerFailure_returnsFalseAndDoesNotThrow() {
        SendGridMailService service = service(true, "SG.test");
        // The factory above always throws IOException; the service must swallow it.
        assertThat(service.send("buyer@example.com", "Subject", "body")).isFalse();
        assertThat(service.sendHtml("buyer@example.com", "Subject", "<p>body</p>")).isFalse();
    }

    @Test
    void successPath_invokesProviderAndReturnsTrue() {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(true);
        properties.setApiKey("SG.test");
        properties.setFromEmail("no-reply@kilivana.com");
        properties.setFromName("Kilivana");

        AtomicReference<String> receivedKey = new AtomicReference<>();
        AtomicReference<String> receivedEndpoint = new AtomicReference<>();
        SendGrid stub = new SendGrid("SG.test") {
            @Override
            public Response api(Request request) {
                receivedEndpoint.set(request.getEndpoint());
feature/supplier-api-updates
                Response response = new Response(); response.setStatusCode(202); return response;

                return new Response(202, "", null); main
            }
        };

        SendGridMailService service = new SendGridMailService(properties, key -> {
            receivedKey.set(key);
            return stub;
        });

        boolean sent = service.send("buyer@example.com", "Subject", "body");

        assertThat(sent).isTrue();
        assertThat(receivedKey.get()).isEqualTo("SG.test");
        assertThat(receivedEndpoint.get()).isEqualTo("mail/send");
    }

feature/supplier-api-updates

    @Test void rejectedAndRuntimeProviderResponsesFailWithoutLoggingRequestSecrets() {
        SendGridProperties p=new SendGridProperties();p.setEnabled(true);p.setApiKey("test-provider-secret");
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(SendGridMailService.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try {
            SendGrid rejected=new SendGrid("test") { @Override public Response api(Request r) {
                Response response=new Response();response.setStatusCode(429);return response;
            }};
            assertThat(new SendGridMailService(p,k->rejected).sendHtml("test@example.invalid","Verification","123456")).isFalse();
            var failed=new SendGridMailService(p,k->{throw new IllegalArgumentException("test-provider-secret 123456");});
            assertThat(failed.sendHtml("test@example.invalid","Verification","123456")).isFalse();
            assertThat(appender.list).allMatch(e->!e.getFormattedMessage().contains("123456")
                && !e.getFormattedMessage().contains("test-provider-secret") && e.getThrowableProxy()==null);
        } finally {logger.detachAppender(appender);appender.stop();}

    @Test
    void providerRefusal_returnsFalseRatherThanReportingASend() {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(true);
        properties.setApiKey("SG.test");
        properties.setFromEmail("no-reply@kilivana.com");
        properties.setFromName("Kilivana");

        // SendGrid reports an unverified sender as 403 with the reason in the body.
        SendGrid refusing = new SendGrid("SG.test") {
            @Override
            public Response api(Request request) {
                return new Response(403,
                        "{\"errors\":[{\"message\":\"The from address does not match a verified "
                                + "Sender Identity.\"}]}",
                        null);
            }
        };

        SendGridMailService service = new SendGridMailService(properties, key -> refusing);

        assertThat(service.sendHtml("buyer@example.com", "Subject", "<p>body</p>")).isFalse(); main
    }

    /** A SendGrid whose {@code api()} always fails with a checked IOException. */
    private static final class FailingSendGrid extends SendGrid {
        private final String key;

        private FailingSendGrid(String key) {
            super(key);
            this.key = key;
        }

        @Override
        public Response api(Request request) throws IOException {
            throw new IOException("simulated network failure for " + key);
        }
    }
}
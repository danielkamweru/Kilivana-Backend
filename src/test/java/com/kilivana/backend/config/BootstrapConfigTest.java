package com.kilivana.backend.config;

import com.kilivana.backend.mail.SendGridBootstrapValidator;
import com.kilivana.backend.mail.SendGridProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Verifies the bootstrap-time configuration behaviour the audit cares about:
 * the .env loader exposes its values, and the SendGrid validator fails fast.
 */
class BootstrapConfigTest {

    private static final ApplicationArguments NO_ARGS = mock(ApplicationArguments.class);

    @Test
    void dotenvPostProcessorExposesEntriesFromTheEnvFile() throws IOException {
        Path tmp = Files.createTempFile("dotenv-test", ".env");
        Files.writeString(tmp, """
                DARAJA_CONSUMER_KEY=from-file
                SENDGRID_API_KEY=SG.test
                """);

        try {
            DotenvEnvironmentPostProcessor p = new DotenvEnvironmentPostProcessor() {
                @Override
                protected Path resolveEnvFile() {
                    return tmp;
                }
            };

            MockEnvironment env = new MockEnvironment();
            p.postProcessEnvironment(env, new SpringApplication());

            assertThat(env.getProperty("DARAJA_CONSUMER_KEY")).isEqualTo("from-file");
            assertThat(env.getProperty("SENDGRID_API_KEY")).isEqualTo("SG.test");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void dotenvPostProcessorIsNoOpWhenTheFileIsMissing() {
        DotenvEnvironmentPostProcessor p = new DotenvEnvironmentPostProcessor() {
            @Override
            protected Path resolveEnvFile() {
                return Path.of("/no/such/.env");
            }
        };
        MockEnvironment env = new MockEnvironment();
        p.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("DARAJA_CONSUMER_KEY")).isNull();
    }

    @Test
    void sendGridBootstrapValidatorFailsWhenEnabledWithoutAKey() {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(true);
        properties.setApiKey("");
        properties.setFromEmail("no-reply@kilivana.com");

        SendGridBootstrapValidator validator = new SendGridBootstrapValidator(properties);

        assertThrows(IllegalStateException.class, () -> validator.run(NO_ARGS));
    }

    @Test
    void sendGridBootstrapValidatorFailsWhenEnabledWithoutASender() {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(true);
        properties.setApiKey("SG.test");
        properties.setFromEmail("");

        SendGridBootstrapValidator validator = new SendGridBootstrapValidator(properties);

        assertThrows(IllegalStateException.class, () -> validator.run(NO_ARGS));
    }

    @Test
    void sendGridBootstrapValidatorPassesWhenFullyConfigured() {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(true);
        properties.setApiKey("SG.test");
        properties.setFromEmail("no-reply@kilivana.com");

        SendGridBootstrapValidator validator = new SendGridBootstrapValidator(properties);

        validator.run(NO_ARGS);
    }

    @Test
    void sendGridBootstrapValidatorIsSkippedWhenDisabled() {
        SendGridProperties properties = new SendGridProperties();
        properties.setEnabled(false);
        properties.setFromEmail("");

        SendGridBootstrapValidator validator = new SendGridBootstrapValidator(properties);

        validator.run(NO_ARGS);
    }
}
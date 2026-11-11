package com.kilivana.backend.mail;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentOtpLoggerTest {
    @Test
    void loggerExistsOnlyWithDevOptInAndWithoutProductionProfiles() {
        assertLoggerAvailable(new String[]{"dev"}, true);
        assertLoggerAvailable(new String[]{"dev", "prod"}, false);
        assertLoggerAvailable(new String[]{"dev", "production"}, false);
        assertLoggerAvailable(new String[]{"prod"}, false);
        assertLoggerAvailable(new String[0], false);
    }

    @Test
    void devProfileMustAlsoHaveItsExplicitConfigurationProperty() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("dev");
            context.register(DevelopmentOtpLogger.class);
            context.refresh();
            assertThat(context.getBeansOfType(DevelopmentOtpLogger.class)).isEmpty();
        }
    }

    @Test
    void logContainsRecoveryDetailsForLocalTerminal() {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(DevelopmentOtpLogger.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try {
            new DevelopmentOtpLogger().log("farmer@example.test","EMAIL_VERIFICATION",
                    Instant.parse("2026-11-11T20:00:00Z"),"001234");
            assertThat(appender.list).singleElement().satisfies(event ->
                    assertThat(event.getFormattedMessage()).contains("farmer@example.test",
                            "EMAIL_VERIFICATION","2026-11-11T20:00:00Z","001234"));
        } finally {logger.detachAppender(appender);appender.stop();}
    }

    @Test
    void synchronousOtpIsAttachedToHttpRequestForStatusLine() {
        var request=new MockHttpServletRequest("POST","/api/v1/auth/register");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            new DevelopmentOtpLogger().log("farmer@example.test","EMAIL_VERIFICATION",
                    Instant.parse("2026-11-11T20:00:00Z"),"001234");
            assertThat(request.getAttribute(DevelopmentOtpLogger.REQUEST_ATTRIBUTE).toString())
                    .contains("farmer@example.test","EMAIL_VERIFICATION","001234");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private void assertLoggerAvailable(String[] profiles, boolean expected) {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profiles);
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("app.email-verification.log-otp", "true")));
            context.register(DevelopmentOtpLogger.class);
            context.refresh();
            assertThat(context.getBeansOfType(DevelopmentOtpLogger.class).isEmpty()).isEqualTo(!expected);
        }
    }
}

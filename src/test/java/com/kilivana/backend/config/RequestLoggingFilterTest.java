package com.kilivana.backend.config;

import com.kilivana.backend.mail.DevelopmentOtpLogger;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the request-logging filter: a client-supplied trace id is preserved on the
 * response, and when none is supplied the server generates one and echoes it back.
 * These are the only externally observable behaviours; the log lines themselves are
 * verified by reading the terminal during a manual run.
 */
@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(
        com.kilivana.backend.common.controller.HealthController.class)
@org.springframework.context.annotation.Import(SecurityConfig.class)
class RequestLoggingFilterTest {
    @org.springframework.boot.test.mock.mockito.MockBean
    com.kilivana.backend.security.AccountAccessValidator accountAccessValidator;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void echoesClientSuppliedRequestId() throws Exception {
        mockMvc.perform(get("/").header("X-Request-ID", "trace-abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "trace-abc-123"));
    }

    @Test
    void generatesRequestIdWhenAbsent() throws Exception {
        var result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();
        String traceId = result.getResponse().getHeader("X-Request-ID");
        assertThat(traceId, not(emptyString()));
        assertThat(traceId, hasLength(36)); // UUID string length
    }

    @Test
    void requestStatusLineIncludesOtpWhenDevelopmentRequestAttributeIsPresent() throws Exception {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(RequestLoggingFilter.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try {
            var request=new MockHttpServletRequest("POST","/api/v1/auth/register");
            request.setAttribute(DevelopmentOtpLogger.REQUEST_ATTRIBUTE,
                    "LOCAL DEVELOPMENT OTP recipient=farmer@example.test purpose=EMAIL_VERIFICATION "
                            + "expiresAt=" + Instant.parse("2026-11-11T20:00:00Z") + " code=001234");
            var response=new MockHttpServletResponse();
            response.setStatus(201);
            FilterChain chain=(req,res)->((jakarta.servlet.http.HttpServletResponse)res).setStatus(201);

            new RequestLoggingFilter().doFilter(request,response,chain);

            org.assertj.core.api.Assertions.assertThat(appender.list).anySatisfy(event ->
                    org.assertj.core.api.Assertions.assertThat(event.getFormattedMessage()).contains(
                            "POST /api/v1/auth/register -> 201",
                            "LOCAL DEVELOPMENT OTP",
                            "farmer@example.test",
                            "EMAIL_VERIFICATION",
                            "001234"));
            assertNull(request.getAttribute(DevelopmentOtpLogger.REQUEST_ATTRIBUTE));
        } finally {logger.detachAppender(appender);appender.stop();}
    }
}
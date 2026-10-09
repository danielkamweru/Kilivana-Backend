package com.kilivana.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
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
}
package com.kilivana.backend.common.controller;

import com.kilivana.backend.common.exception.TooManyRequestsException;
import com.kilivana.backend.common.service.AuthService;
import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.security.AuthRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerRateLimitTest {

    @MockBean private com.kilivana.backend.security.AccountAccessValidator accountAccessValidator;
    @MockBean private AuthService authService;
    @MockBean private AuthRateLimiter authRateLimiter;
    @MockBean private com.kilivana.backend.common.service.EmailOtpRequestLimiter emailOtpRequestLimiter;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void throttledLoginIsRefusedWithTooManyRequests() throws Exception {
        doThrow(new TooManyRequestsException("Too many attempts. Please wait and try again."))
                .when(authRateLimiter).enforce(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"daniel@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void throttledRecoveryRequestIsRefusedWithTooManyRequests() throws Exception {
        doThrow(new TooManyRequestsException("Too many attempts. Please wait and try again."))
                .when(authRateLimiter).enforce(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password").param("email", "daniel@example.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }
}

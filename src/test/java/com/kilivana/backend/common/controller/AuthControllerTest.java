package com.kilivana.backend.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.common.dto.AuthLoginRequest;
import com.kilivana.backend.common.dto.AuthTokenResponse;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.common.service.AuthService;
import com.kilivana.backend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.kilivana.backend.common.dto.PasswordResetRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @MockBean private com.kilivana.backend.common.service.EmailOtpRequestLimiter emailOtpRequestLimiter;
    @MockBean private com.kilivana.backend.security.AccountAccessValidator accountAccessValidator;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/reset-password", "/api/auth/reset-password"})
    void anonymousPasswordReset_shouldForwardTokenRequest(String route) throws Exception {
        when(authService.resetPassword(any(PasswordResetRequest.class)))
                .thenReturn("Password has been reset successfully.");
        for (String email : new String[]{"daniel@example.com", "unknown@example.com"}) {
            mockMvc.perform(post(route).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(PasswordResetRequest.builder()
                                    .token("valid-test-token").newPassword("attacker123").build())))
                    .andExpect(status().isOk())
                    
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/reset-password", "/api/auth/reset-password"})
    void invalidPasswordReset_shouldRetainValidation(String route) throws Exception {
        mockMvc.perform(post(route).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\",\"newPassword\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        org.mockito.Mockito.verifyNoInteractions(authService);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .name("Daniel")
                .email("daniel@example.com")
                .phone("0712345678")
                .password("secret123")
                .role(UserRole.BUYER)
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .name("Daniel")
                .email("daniel@example.com")
                .phone("0712345678")
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.register(any(UserRegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("daniel@example.com"));
    }

    @Test
    void login_shouldReturnTokens() throws Exception {
        AuthLoginRequest request = AuthLoginRequest.builder()
                .email("daniel@example.com")
                .password("secret123")
                .build();
        AuthTokenResponse response = AuthTokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .build();

        when(authService.login(any(AuthLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void login_shouldReturnAcceptedOtpChallengeForUnverifiedAccount() throws Exception {
        AuthLoginRequest request = AuthLoginRequest.builder()
                .email("daniel@example.com")
                .password("secret123")
                .build();
        var challengeId = java.util.UUID.randomUUID();
        when(authService.login(any(AuthLoginRequest.class))).thenReturn(AuthTokenResponse.builder()
                .emailVerificationRequired(true)
                .emailVerificationChallengeId(challengeId)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.emailVerificationRequired").value(true))
                .andExpect(jsonPath("$.data.emailVerificationChallengeId").value(challengeId.toString()))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());
    }

    @Test
    void legacyAuthAlias_shouldSupportLogin() throws Exception {
        AuthLoginRequest request = AuthLoginRequest.builder()
                .email("daniel@example.com")
                .password("secret123")
                .build();
        when(authService.login(any(AuthLoginRequest.class))).thenReturn(AuthTokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }
}

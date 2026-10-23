package com.kilivana.backend.common.controller;

import com.kilivana.backend.admin.controller.*;
import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.dto.*;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.exception.*;
import com.kilivana.backend.common.repository.PasswordResetTokenRepository;
import com.kilivana.backend.common.service.*;
import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.mail.SendGridMailService;
import com.kilivana.backend.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@WebMvcTest({EmailVerificationController.class,AuthController.class,UserProfileController.class,UserController.class})
@Import({SecurityConfig.class,AuthService.class,AccountAccessValidator.class})
class EmailVerificationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;
    @MockBean private com.kilivana.backend.common.service.EmailOtpRequestLimiter emailOtpRequestLimiter;
    @MockBean UserRepository users;
    @MockBean UserService userService;
    @MockBean PasswordEncoder passwords;
    @MockBean UserReferenceCodeGenerator references;
    @MockBean PasswordResetTokenRepository resetTokens;
    @MockBean SendGridMailService mail;
    @MockBean EmailVerificationService verification;
    final UUID challengeId=UUID.randomUUID();
    @BeforeEach void setup() {
        when(users.save(any())).thenAnswer(i->{User u=i.getArgument(0);u.setId(7L);return u;});
        when(passwords.encode(any())).thenReturn("test-hash");
        when(verification.startForNewAccount(any())).thenReturn(EmailOtpChallengeResponse.accepted(challengeId));
        when(verification.request(anyString())).thenReturn(EmailOtpChallengeResponse.accepted(challengeId));
    }
    String registration(UserRole role) throws Exception {
        return json.writeValueAsString(UserRegistrationRequest.builder().name("OTP Fixture").email("test@example.invalid")
            .phone("0700000000").password("test-password").role(role).build());
    }
    @ParameterizedTest @ValueSource(strings={"/api/v1/auth/register","/api/auth/register","/api/v1/users","/api/users"})
    void allRegistrationAliasesRejectPublicInspectorAndAdmin(String route) throws Exception {
        for(var role:List.of(UserRole.INSPECTOR,UserRole.ADMIN))
            mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(registration(role)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verify(users,never()).save(any());verifyNoInteractions(verification);
    }
    @ParameterizedTest @ValueSource(strings={"FARMER","SUPPLIER","BUYER","DRIVER"})
    void permittedRolesGetUnverifiedAccountAndChallengeButNoTokens(String role) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration(UserRole.valueOf(role))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.emailVerified").value(false))
            .andExpect(jsonPath("$.data.emailVerificationChallengeId").value(challengeId.toString()))
            .andExpect(jsonPath("$.data.accessToken").doesNotExist()).andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }
    @Test void suppliedEmailVerifiedFlagCannotBypassRegistrationGate() throws Exception {
        String body=registration(UserRole.FARMER).replaceFirst("\\{","{\"emailVerified\":true,\"verificationStatus\":\"VERIFIED\",");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.emailVerified").value(false));
    }
    @ParameterizedTest @ValueSource(strings={"/api/v1/auth/email-verification","/api/auth/email-verification"})
    void otpRoutesArePublicWithConsistentPayloads(String prefix) throws Exception {
        for(var action:List.of("request","resend"))
            mvc.perform(post(prefix+"/"+action).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"test@example.invalid\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.challengeId").value(challengeId.toString()))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(600)).andExpect(jsonPath("$.data.resendAfterSeconds").value(60))
                .andExpect(jsonPath("$.data.code").doesNotExist());
        mvc.perform(post(prefix+"/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.invalid\",\"challengeId\":\""+challengeId+"\",\"code\":\"123456\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }
    @Test void invalidCodeHasGenericErrorAndRateLimitHasRetryAfter() throws Exception {
        doThrow(new EmailOtpInvalidException()).when(verification).verify(any());
        mvc.perform(post("/api/v1/auth/email-verification/verify").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"test@example.invalid\",\"challengeId\":\""+challengeId+"\",\"code\":\"123456\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid or expired email verification challenge"));
        when(verification.request(anyString())).thenThrow(new EmailOtpRateLimitException(60));
        for(String email:List.of("test@example.invalid","unknown@example.invalid"))
            mvc.perform(post("/api/v1/auth/email-verification/resend").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","60"))
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"));
    }
    @Test void malformedVerificationRequestIsValidatedBeforeService() throws Exception {
        mvc.perform(post("/api/v1/auth/email-verification/verify").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"invalid\",\"code\":\"short\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(verification,never()).verify(any());
    }
    @Test void adminProvisioningIsNotPublicOrAvailableToFarmer() throws Exception {
        mvc.perform(post("/api/v1/admin/users").contentType(MediaType.APPLICATION_JSON).content(registration(UserRole.INSPECTOR)))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/users").with(user("farmer").roles("FARMER"))
            .contentType(MediaType.APPLICATION_JSON).content(registration(UserRole.INSPECTOR))).andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }
    @Test void unverifiedEmailBlocksLoginRefreshAndSignedAccessToken() throws Exception {
        User account=User.builder().id(7L).name("Fixture").email("test@example.invalid").role(UserRole.FARMER)
            .status(UserStatus.ACTIVE).passwordHash("test-hash").emailVerified(false).build();
        when(users.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
        when(users.findById(7L)).thenReturn(Optional.of(account));when(passwords.matches(any(),any())).thenReturn(true);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"test@example.invalid\",\"password\":\"test-password\"}"))
            .andExpect(status().isAccepted());
        String refresh=jwt.generateRefreshToken(account);
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+refresh+"\"}"))
            .andExpect(status().isForbidden());
        String token=jwt.generateAccessToken(account);
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        account.setEmailVerified(true);
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
    }
}

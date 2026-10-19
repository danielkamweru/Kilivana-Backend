package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.common.dto.AuthLoginRequest;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.ConflictException;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceEmailNormalizationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserReferenceCodeGenerator referenceCodeGenerator;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtService,
                new JwtProperties(), referenceCodeGenerator, mock(com.kilivana.backend.common.repository.PasswordResetTokenRepository.class), mock(com.kilivana.backend.mail.SendGridMailService.class));
    }

    @Test void pendingFarmerCanLoginAndRefreshWithoutChangingVerification() {
        User user = User.builder().id(7L).email("pending@example.com").role(UserRole.FARMER)
                .status(com.kilivana.backend.common.enums.UserStatus.PENDING_VERIFICATION)
                .verificationStatus(com.kilivana.backend.common.enums.VerificationStatus.PENDING)
                .passwordHash(new BCryptPasswordEncoder().encode("probe12345")).build();
        when(userRepository.findByEmailIgnoreCase("pending@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(jwtService.isTokenType("refresh", JwtService.TYPE_REFRESH)).thenReturn(true);
        when(jwtService.extractUserId("refresh")).thenReturn(7L);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        AuthLoginRequest login = new AuthLoginRequest();
        login.setEmail("pending@example.com"); login.setPassword("probe12345");
        assertThat(authService.login(login).getAccessToken()).isEqualTo("access");
        assertThat(authService.refreshToken("refresh").getAccessToken()).isEqualTo("access");
        assertThat(user.getVerificationStatus()).isEqualTo(com.kilivana.backend.common.enums.VerificationStatus.PENDING);
    }

    private static UserRegistrationRequest request(String email, String phone) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("Case Probe");
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword("probe12345");
        request.setRole(UserRole.DRIVER);
        return request;
    }

    @Test
    void register_shouldStoreEmailLowercasedAndTrimmed() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.countByRole(any())).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        authService.register(request("  Jane.Example@Example.COM ", "0700000011"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("jane.example@example.com");
        assertThat(captor.getValue().getPhone()).isEqualTo("0700000011");
    }

    @Test
    void register_shouldRejectTheSameAddressInAnotherCase() {
        // The generated unique constraint is case-sensitive in PostgreSQL, so before
        // normalization both spellings registered as separate accounts.
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request("JANE.EXAMPLE@EXAMPLE.COM", "0700000012")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void login_shouldMatchRegardlessOfCase() {
        User user = User.builder()
                .id(7L)
                .email("jane.example@example.com")
                .passwordHash(new BCryptPasswordEncoder().encode("probe12345"))
                .role(UserRole.DRIVER)
                .build();
        when(userRepository.findByEmailIgnoreCase("jane.example@example.com"))
                .thenReturn(Optional.of(user));

        AuthLoginRequest login = new AuthLoginRequest();
        login.setEmail("Jane.Example@EXAMPLE.com");
        login.setPassword("probe12345");

        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        assertThat(authService.login(login).getAccessToken()).isEqualTo("access");
    }
}
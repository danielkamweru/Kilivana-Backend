package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.PasswordResetRequest;
import com.kilivana.backend.common.entity.PasswordResetToken;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.TooManyRequestsException;
import com.kilivana.backend.common.repository.PasswordResetTokenRepository;
import com.kilivana.backend.mail.SendGridMailService;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Drives the password-reset flow end to end, without touching a mail provider.
 *
 * <p>Covers token generation, hashing, expiry, single-use, password hashing and
 * the non-enumerating forgot-password response.
 */
class PasswordResetFlowTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService service(UserRepository users, PasswordResetTokenRepository tokens,
                                SendGridMailService mail, PasswordEncoder encoder) {
        AuthService s = new AuthService(users, encoder, mock(JwtService.class), new JwtProperties(),
                mock(com.kilivana.backend.common.service.UserReferenceCodeGenerator.class),
                tokens, mail);
        // The reset link is only built when a frontend URL is configured; the default
        // test environment has none, so point it at a placeholder host.
        s.setPasswordResetFrontendUrl("https://example.com/reset");
        return s;
    }

    @Test
    void forgotPasswordGeneratesASingleUseHashedTokenAndSendsMail() {
        User user = User.builder().id(7L).email("daniel@example.com").build();
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase("daniel@example.com")).thenReturn(Optional.of(user));

        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SendGridMailService mail = mock(SendGridMailService.class);
        when(mail.sendHtml(any(), any(), any())).thenReturn(true);

        String response = service(users, tokens, mail, passwordEncoder).forgotPassword("Daniel@example.com");

        verify(tokens).deleteByUserId(7L);
        ArgumentCaptor<PasswordResetToken> tokenCaptor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getTokenHash()).isNotNull();
        assertThat(saved.getTokenHash()).isNotEqualTo("");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.getUsedAt()).isNull();
        verify(mail).sendHtml(org.mockito.ArgumentMatchers.eq("daniel@example.com"),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.contains("token="));
        assertThat(response).contains("reset instructions");
    }

    @Test
    void tokenIsSingleUseAndConsumedOnSuccess() {
        String raw = "raw-token-value";
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(7L).tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.findByTokenHashForUpdate(hash(raw))).thenReturn(Optional.of(token));
        User user = User.builder()
                .id(7L).passwordHash(passwordEncoder.encode("old-secret")).build();
        UserRepository users = mock(UserRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(user));

        String message = service(users, tokens, mock(SendGridMailService.class), passwordEncoder)
                .resetPassword(PasswordResetRequest.builder().token(raw).newPassword("brand-new-123").build());

        assertThat(message).contains("reset successfully");
        assertThat(token.getUsedAt()).isAfter(LocalDateTime.now().minusSeconds(5));
        assertThat(passwordEncoder.matches("brand-new-123", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("old-secret", user.getPasswordHash())).isFalse();
    }

    @Test
    void expiredTokenIsRefused() {
        String raw = "raw-token-value";
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(7L).tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.findByTokenHashForUpdate(hash(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service(mock(UserRepository.class), tokens,
                mock(SendGridMailService.class), passwordEncoder).resetPassword(
                        PasswordResetRequest.builder().token(raw).newPassword("new-123456").build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void alreadyUsedTokenIsRefused() {
        String raw = "raw-token-value";
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(7L).tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .usedAt(LocalDateTime.now().minusMinutes(2))
                .build();
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.findByTokenHashForUpdate(hash(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service(mock(UserRepository.class), tokens,
                mock(SendGridMailService.class), passwordEncoder).resetPassword(
                        PasswordResetRequest.builder().token(raw).newPassword("new-123456").build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownEmailDoesNotEnumerateAccounts() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        String response = service(users, mock(PasswordResetTokenRepository.class),
                mock(SendGridMailService.class), passwordEncoder).forgotPassword("nobody@example.com");

        assertThat(response).contains("reset instructions");
        verify(users).findByEmailIgnoreCase("nobody@example.com");
    }

    @Test
    void aSecondRequestInsideTheCooldownIsRefused() {
        User user = User.builder().id(7L).email("daniel@example.com").build();
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase("daniel@example.com")).thenReturn(Optional.of(user));

        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SendGridMailService mail = mock(SendGridMailService.class);
        when(mail.sendHtml(any(), any(), any())).thenReturn(true);

        AuthService service = service(users, tokens, mail, passwordEncoder);
        service.setPasswordResetCooldownMinutes(60);
        service.forgotPassword("daniel@example.com");

        assertThatThrownBy(() -> service.forgotPassword("daniel@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(tokens, times(1)).save(any());
    }

    @Test
    void theCooldownAppliesToUnknownAddressesToo() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        AuthService service = service(users, mock(PasswordResetTokenRepository.class),
                mock(SendGridMailService.class), passwordEncoder);
        service.setPasswordResetCooldownMinutes(60);
        service.forgotPassword("nobody@example.com");

        assertThatThrownBy(() -> service.forgotPassword("nobody@example.com"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void aCooldownOfZeroDisablesTheThrottle() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        AuthService service = service(users, mock(PasswordResetTokenRepository.class),
                mock(SendGridMailService.class), passwordEncoder);
        service.setPasswordResetCooldownMinutes(0);

        assertThat(service.forgotPassword("nobody@example.com")).contains("reset instructions");
        assertThat(service.forgotPassword("nobody@example.com")).contains("reset instructions");
    }

    @Test
    void aFailedDeliveryReleasesTheCooldown() {
        User user = User.builder().id(7L).email("daniel@example.com").build();
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase("daniel@example.com")).thenReturn(Optional.of(user));

        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SendGridMailService mail = mock(SendGridMailService.class);
        when(mail.sendHtml(any(), any(), any())).thenReturn(false);

        AuthService service = service(users, tokens, mail, passwordEncoder);
        service.setPasswordResetCooldownMinutes(60);
        service.forgotPassword("daniel@example.com");

        assertThat(service.forgotPassword("daniel@example.com")).contains("reset instructions");
        // The undelivered token is withdrawn rather than left usable.
        verify(tokens, atLeastOnce()).delete(any(PasswordResetToken.class));
    }

    @Test
    void aFailedDeliveryKeepsTheTokenWhenTheDevelopmentLinkIsLogged() {
        User user = User.builder().id(7L).email("daniel@example.com").build();
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase("daniel@example.com")).thenReturn(Optional.of(user));

        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SendGridMailService mail = mock(SendGridMailService.class);
        when(mail.sendHtml(any(), any(), any())).thenReturn(false);

        AuthService service = service(users, tokens, mail, passwordEncoder);
        service.setDevLogResetLink(true);
        service.forgotPassword("daniel@example.com");

        // The link is in the log, so the token must survive for the flow to be completed.
        verify(tokens, never()).delete(any(PasswordResetToken.class));
    }

    @Test
    void aPasswordShorterThanThePolicyIsRefused() {
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        assertThatThrownBy(() -> service(mock(UserRepository.class), tokens, mock(SendGridMailService.class), encoder).resetPassword(
                PasswordResetRequest.builder().token("raw-token-value").newPassword("short12").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 8");
        verifyNoInteractions(encoder);
    }

    private static String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
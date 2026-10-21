package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.PasswordResetRequest;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.repository.PasswordResetTokenRepository;
import com.kilivana.backend.mail.SendGridMailService;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetSecurityTest {
    @Test
    void unknownEmailGetsGenericRecoveryResponse() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        AuthService service = service(users, mock(PasswordResetTokenRepository.class), mock(PasswordEncoder.class));
        assertThat(service.forgotPassword("unknown@example.com"))
                .isEqualTo("If the account exists and email delivery is available, reset instructions will be sent.");
    }

    @Test
    void invalidTokenCannotChangePassword() {
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        assertThatThrownBy(() -> service(mock(UserRepository.class), tokens, encoder).resetPassword(
                PasswordResetRequest.builder().token("invalid").newPassword("attacker123").build()))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(encoder);
    }

    private static AuthService service(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder) {
        return new AuthService(users, encoder, mock(JwtService.class), new JwtProperties(),
                mock(UserReferenceCodeGenerator.class), tokens, mock(SendGridMailService.class), mock(EmailVerificationService.class));
    }
}

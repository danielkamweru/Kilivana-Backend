package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.PasswordResetRequest;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
import com.kilivana.backend.security.JwtProperties;
import com.kilivana.backend.security.JwtService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PasswordResetSecurityTest {
    @ParameterizedTest
    @ValueSource(strings = {"known@example.com", "unknown@example.com"})
    void forgotResponseDoesNotRevealOrReadAccountExistence(String email) {
        UserRepository repository = mock(UserRepository.class);
        AuthService service = new AuthService(repository, mock(PasswordEncoder.class), mock(JwtService.class),
                new JwtProperties(), mock(UserReferenceCodeGenerator.class));
        org.assertj.core.api.Assertions.assertThat(service.forgotPassword(email))
                .isEqualTo("Password reset instructions have been sent if this account exists.");
        verifyNoInteractions(repository);
    }
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"daniel@example.com", "unknown@example.com", " DANIEL@EXAMPLE.COM ", ""})
    void resetIsDisabledWithoutReadingOrMutatingAccounts(String email) {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        UserReferenceCodeGenerator codes = mock(UserReferenceCodeGenerator.class);
        AuthService service = new AuthService(repository, encoder, jwt, new JwtProperties(), codes);

        assertThatThrownBy(() -> service.resetPassword(
                PasswordResetRequest.builder().email(email).newPassword("attacker123").build()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Password reset is currently unavailable.");
        assertThatThrownBy(() -> service.resetPassword(null))
                .isInstanceOf(ServiceUnavailableException.class);
        verifyNoInteractions(repository, encoder, jwt, codes);
    }
}

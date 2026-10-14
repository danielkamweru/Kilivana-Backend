package com.kilivana.backend.security;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountAccessValidatorTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AccountAccessValidator validator = new AccountAccessValidator(users);
    @Test void validAccountKeepsExistingTokenContract() {
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().role(UserRole.FARMER).status(UserStatus.ACTIVE).build()));
        assertDoesNotThrow(() -> validator.validate(1L, UserRole.FARMER));
    }
    @Test void suspendedInactiveAndMissingAccountsCannotUseIssuedAccessTokens() {
        for (UserStatus status : new UserStatus[]{UserStatus.SUSPENDED, UserStatus.INACTIVE}) {
            when(users.findById(1L)).thenReturn(Optional.of(User.builder().role(UserRole.FARMER).status(status).build()));
            assertThrows(UnauthorizedException.class, () -> validator.validate(1L, UserRole.FARMER));
        }
        when(users.findById(1L)).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> validator.validate(1L, UserRole.FARMER));
    }
    @Test void staleRoleTokenRejected() {
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().role(UserRole.BUYER).status(UserStatus.ACTIVE).build()));
        assertThrows(UnauthorizedException.class, () -> validator.validate(1L, UserRole.ADMIN));
    }
}

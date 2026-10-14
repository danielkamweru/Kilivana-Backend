package com.kilivana.backend.security;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Existing JWT wire format, but account and role must still be valid now. */
@Component
@RequiredArgsConstructor
public class AccountAccessValidator {
    private final UserRepository users;

    public void validate(Long id, UserRole tokenRole) {
        var user = users.findById(id).orElseThrow(() -> new UnauthorizedException("Invalid access token"));
        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() != tokenRole) {
            throw new UnauthorizedException("Invalid access token");
        }
    }
}

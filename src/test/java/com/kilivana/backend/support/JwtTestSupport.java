package com.kilivana.backend.support;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import com.kilivana.backend.security.JwtService;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Builds real signed access tokens for web-layer tests.
 *
 * <p>Tokens are signed with the application's own {@link JwtService} bean so they
 * pass the same {@code JwtAuthenticationFilter} that serves production traffic,
 * rather than bypassing authentication in the test.
 */
public final class JwtTestSupport {

    private JwtTestSupport() {
    }

    /** A {@code Authorization: Bearer ...} processor for the given identity. */
    public static RequestPostProcessor asUser(JwtService jwtService, Long userId, UserRole role) {
        return asUser(jwtService, userId, role, "test@kilivana.local");
    }

    public static RequestPostProcessor asUser(JwtService jwtService, Long userId, UserRole role, String email) {
        String token = jwtService.generateAccessToken(user(userId, role, email));
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    public static RequestPostProcessor asFarmer(JwtService jwtService, Long userId) {
        return asUser(jwtService, userId, UserRole.FARMER);
    }

    public static RequestPostProcessor asBuyer(JwtService jwtService, Long userId) {
        return asUser(jwtService, userId, UserRole.BUYER);
    }

    public static RequestPostProcessor asDriver(JwtService jwtService, Long userId) {
        return asUser(jwtService, userId, UserRole.DRIVER);
    }

    public static RequestPostProcessor asInspector(JwtService jwtService, Long userId) {
        return asUser(jwtService, userId, UserRole.INSPECTOR);
    }

    public static RequestPostProcessor asSupplier(JwtService jwtService, Long userId) {
        return asUser(jwtService, userId, UserRole.SUPPLIER);
    }

    public static RequestPostProcessor asAdmin(JwtService jwtService, Long userId) {
        return asUser(jwtService, userId, UserRole.ADMIN);
    }

    private static User user(Long id, UserRole role, String email) {
        return User.builder()
                .id(id)
                .name("Test " + role)
                .email(email)
                .phone("0700000000")
                .role(role)
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.NOT_REQUIRED)
                .build();
    }
}

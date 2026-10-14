package com.kilivana.backend.security;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtAccountEnforcementTest {
    final UserRepository users = mock(UserRepository.class);
    final JwtService jwt = mock(JwtService.class);
    final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, new AccountAccessValidator(users));
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void request(UserStatus status, UserRole currentRole, boolean accessToken) throws Exception {
        when(jwt.isTokenType("test-credential", JwtService.TYPE_ACCESS)).thenReturn(accessToken);
        when(jwt.extractUserId("test-credential")).thenReturn(1L);
        when(jwt.extractRole("test-credential")).thenReturn(UserRole.FARMER);
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).role(currentRole).status(status).build()));
        var req = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        req.addHeader("Authorization", "Bearer test-credential");
        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> {});
    }
    @Test void activeFarmerRetainsNumericPrincipalAndRole() throws Exception {
        request(UserStatus.ACTIVE, UserRole.FARMER, true);
        assertEquals(1L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
    @Test void existingTokenDoesNotAuthenticateSuspendedAccount() throws Exception {
        request(UserStatus.SUSPENDED, UserRole.FARMER, true);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void changedRoleInvalidatesStaleToken() throws Exception {
        request(UserStatus.ACTIVE, UserRole.BUYER, true);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void refreshTokenCannotActAsAccessCredential() throws Exception {
        request(UserStatus.ACTIVE, UserRole.FARMER, false);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(users);
    }
}

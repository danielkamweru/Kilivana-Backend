package com.kilivana.backend.security;

import com.kilivana.backend.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience accessor for the authenticated identity stored in
 * {@link SecurityContextHolder} by {@link JwtAuthenticationFilter}. Because the
 * filter sets the principal to the user's id, controllers and services can call
 * {@code CurrentUser.id()} to obtain the caller without threading
 * {@code Authentication} through every signature.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * Returns the id of the caller resolved by the JWT filter, or throws
     * {@link com.kilivana.backend.common.exception.UnauthorizedException}
     * when no authentication is present so callers get a 401 instead of a
     * null dereference.
     */
    public static Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new UnauthorizedException("Authentication is required to access this resource");
        }
        return userId;
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}

package com.kilivana.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Resolves the caller on every request from the stateless JWT carried in the
 * {@code Authorization} header. Spring Security is configured as stateless,
 * so there is no session to fall back on; this filter is what populates
 * {@link SecurityContextHolder} so controllers and method security can see who
 * is making the call.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // JWT is stateless: there is no session, so the caller is resolved from
        // the Authorization header on every request.
        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (jwtService.isTokenType(token, JwtService.TYPE_ACCESS)) {
                    authenticate(token, request);
                } else {
                    log.debug("Rejected a non-access token presented as a bearer credential");
                }
            } catch (Exception ex) {
                log.debug("Bearer token was rejected: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Builds the authentication object from the token's claims and stores it
     * in the security context. The principal is the user id — a compact value
     * that {@link CurrentUser} reads back without a DB hit.
     */
    private void authenticate(String token, HttpServletRequest request) {
        Long userId = jwtService.extractUserId(token);
        var role = jwtService.extractRole(token);

        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** Pulls the bearer token from the Authorization header, returning null when absent. */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}

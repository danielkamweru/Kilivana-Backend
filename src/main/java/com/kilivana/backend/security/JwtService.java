package com.kilivana.backend.security;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates and validates the JWTs used for authentication. Issues short-lived
 * access tokens and longer-lived refresh tokens; access tokens carry the
 * user id, role and email so controllers can resolve the caller without a
 * database round-trip, while refresh tokens exist only to mint new access
 * tokens. The signing key is derived once from the configured secret and
 * cached for the lifetime of the process.
 */
@Slf4j
public class JwtService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Builds and caches the HMAC signing key derived from the configured secret.
     * Falls back to an ephemeral key (with a loud warning) when no secret is
     * set, so local development works out of the box without committed
     * credentials. Rejects keys shorter than 256 bits, which would be insecure
     * for HS256.
     */
    private SecretKey signingKey() {
        if (signingKey != null) {
            return signingKey;
        }
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            secret = Base64.getEncoder().encodeToString(random);
            log.warn("app.jwt.secret is not configured. Generated an ephemeral signing key. "
                    + "All issued tokens become invalid on restart. Set the JWT_SECRET environment "
                    + "variable to a stable value outside of development.");
            jwtProperties.setSecret(secret);
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                // Decoded cleanly but is too short: treat the value as raw text instead.
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (DecodingException | IllegalArgumentException ex) {
            // Not Base64. Accept it as a raw UTF-8 secret, which is the common
            // local-development case.
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 256 bits (32 bytes) for HS256 signing. "
                            + "Provide a Base64 or raw value of at least 32 characters.");
        }

        signingKey = Keys.hmacShaKeyFor(keyBytes);
        return signingKey;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, TYPE_ACCESS, jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, TYPE_REFRESH, jwtProperties.getRefreshTokenExpiration());
    }

    /** True when tokens are issued without an {@code exp} claim and therefore never expire. */
    public boolean isAccessTokenNonExpiring() {
        return jwtProperties.getAccessTokenExpiration() <= 0;
    }

    /**
     * Builds a token carrying the user id, role, email and token type as
     * custom claims, so the filter can authenticate subsequent requests
     * without a database lookup. Tokens may be non-expiring when the
     * configured TTL is zero or negative — this is only intended for local
     * development convenience.
     */
    private String generateToken(User user, String type, long expirationMillis) {
        Date now = new Date();
        boolean neverExpires = expirationMillis <= 0;

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId());
        claims.put(CLAIM_ROLE, user.getRole().name());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_TYPE, type);

        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claims(claims)
                .issuedAt(now);

        if (neverExpires) {
            log.warn("Issuing a {} token with no expiry. It stays valid until the JWT "
                    + "signing secret changes.", type);
        } else {
            builder.expiration(new Date(now.getTime() + expirationMillis));
        }

        return builder
                .signWith(signingKey())
                .compact();
    }

    /** Returns the subject claim, which holds the user id string. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        Number userId = claims.get(CLAIM_USER_ID, Number.class);
        if (userId == null) {
            throw new JwtException("Token is missing the user id claim");
        }
        return userId.longValue();
    }

    public UserRole extractRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get(CLAIM_ROLE, String.class);
        if (role == null) {
            throw new JwtException("Token is missing the role claim");
        }
        return UserRole.valueOf(role);
    }

    /**
     * Validates that the token is an access token, not a refresh token.
     * Presenting a refresh token as a bearer credential would be a misuse, so
     * the filter rejects it rather than authenticating the caller.
     */
    public boolean isTokenType(String token, String expectedType) {
        try {
            return expectedType.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Parses and signature-verifies a token, throwing {@link JwtException} on
     * any failure so callers can treat invalid tokens as untrusted.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

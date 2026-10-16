package com.kilivana.backend.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code app.jwt.*} properties that configure token lifetimes and
 * the signing secret. The secret is intentionally left blank by default so
 * that, in development, {@link JwtService} can generate an ephemeral key and
 * warn that issued tokens will not survive a restart.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Signing secret for HS256. When blank, an ephemeral key is generated. */
    private String secret;
    /** Access token lifetime in hours (default: 24 hours). */
    private long accessTokenExpirationHours = 24;
    /** Refresh token lifetime in hours (default: 7 days / 168 hours). */
    private long refreshTokenExpirationHours = 168;

    // Explicit getters and setters for Lombok compatibility
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpirationHours() { return accessTokenExpirationHours; }
    public void setAccessTokenExpirationHours(long accessTokenExpirationHours) {
        this.accessTokenExpirationHours = accessTokenExpirationHours;
    }
    public long getRefreshTokenExpirationHours() { return refreshTokenExpirationHours; }
    public void setRefreshTokenExpirationHours(long refreshTokenExpirationHours) {
        this.refreshTokenExpirationHours = refreshTokenExpirationHours;
    }
}

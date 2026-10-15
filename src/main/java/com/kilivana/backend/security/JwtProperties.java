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
    /** Access token lifetime in milliseconds (default: 30 minutes). */
    private long accessTokenExpiration = 1800000L;
    /** Refresh token lifetime in milliseconds (default: 7 days). */
    private long refreshTokenExpiration = 604800000L;

    // Explicit getters and setters for Lombok compatibility
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}

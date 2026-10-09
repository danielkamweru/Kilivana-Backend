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
    /** Access token lifetime in milliseconds (default: 24 hours). */
    private long accessTokenExpiration = 86400000L;
    /** Refresh token lifetime in milliseconds (default: 7 days). */
    private long refreshTokenExpiration = 604800000L;
}

package com.securebiz.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "securebiz.auth")
public record AuthProperties(
        String issuer,
        String signingKey,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        int maxFailedLogins,
        Duration lockDuration,
        int loginRateLimit,
        Duration loginRateWindow,
        boolean refreshCookieSecure
) {
    public AuthProperties {
        if (signingKey == null || signingKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT signing key must contain at least 32 bytes");
        }
        if (maxFailedLogins < 1 || loginRateLimit < 1) {
            throw new IllegalArgumentException("Authentication limits must be positive");
        }
    }
}

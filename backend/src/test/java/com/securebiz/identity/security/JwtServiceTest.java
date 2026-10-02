package com.securebiz.identity.security;

import com.securebiz.identity.config.AuthProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final AuthProperties properties = new AuthProperties(
            "securebiz-test",
            "0123456789abcdef0123456789abcdef",
            Duration.ofMinutes(15),
            Duration.ofDays(14),
            5,
            Duration.ofMinutes(15),
            10,
            Duration.ofMinutes(1),
            false);

    @Test
    void issuedTokenRoundTripsIdentityAndPermissions() {
        JwtService service = new JwtService(properties);
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        JwtService.IssuedAccessToken issued = service.issue(
                userId, sessionId, "admin", 3L, Set.of("employee.view", "role.manage"));
        SecurityPrincipal parsed = service.parse(issued.value());

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.sessionId()).isEqualTo(sessionId);
        assertThat(parsed.username()).isEqualTo("admin");
        assertThat(parsed.authorizationVersion()).isEqualTo(3L);
        assertThat(parsed.permissions()).containsExactlyInAnyOrder("employee.view", "role.manage");
        assertThat(issued.expiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void modifiedTokenIsRejected() {
        JwtService service = new JwtService(properties);
        String value = service.issue(UUID.randomUUID(), UUID.randomUUID(), "admin", 0L, Set.of()).value();
        String tampered = value.substring(0, value.length() - 2) + "aa";

        assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSigningKeyIsRejected() {
        assertThatThrownBy(() -> new AuthProperties(
                "issuer", "too-short", Duration.ofMinutes(1), Duration.ofDays(1),
                5, Duration.ofMinutes(1), 10, Duration.ofMinutes(1), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }
}

package com.securebiz.identity.security;

import com.securebiz.identity.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {
    private final AuthProperties properties;
    private final SecretKey key;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.signingKey().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedAccessToken issue(UUID userId, UUID sessionId, String username,
                                   long authorizationVersion, Set<String> permissions) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String value = Jwts.builder()
                .issuer(properties.issuer())
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("sid", sessionId.toString())
                .claim("username", username)
                .claim("authv", authorizationVersion)
                .claim("permissions", permissions.stream().sorted().toList())
                .signWith(key)
                .compact();
        return new IssuedAccessToken(value, expiresAt);
    }

    public SecurityPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        List<?> rawPermissions = claims.get("permissions", List.class);
        Set<String> permissions = rawPermissions == null
                ? Set.of()
                : rawPermissions.stream().map(String::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new SecurityPrincipal(
                UUID.fromString(claims.getSubject()),
                UUID.fromString(claims.get("sid", String.class)),
                claims.get("username", String.class),
                claims.get("authv", Number.class).longValue(),
                permissions);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) {}
}

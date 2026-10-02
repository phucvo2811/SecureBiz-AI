package com.securebiz.identity.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        CurrentUser user
) {
    public record CurrentUser(UUID id, String username, String email, Set<String> permissions) {}
}

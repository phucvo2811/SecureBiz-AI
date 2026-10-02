package com.securebiz.identity.model;

import java.time.Instant;
import java.util.UUID;

public record StoredRefreshToken(
        UUID id,
        UUID sessionId,
        UUID userId,
        UUID familyId,
        Instant expiresAt,
        Instant usedAt,
        Instant revokedAt
) {}

package com.securebiz.identity.api;

import java.time.Instant;
import java.util.UUID;

public record AuthSessionResponse(
        UUID id,
        String deviceName,
        String ipAddress,
        String userAgent,
        Instant startedAt,
        Instant lastSeenAt,
        Instant expiresAt,
        Instant revokedAt,
        String revokeReason,
        boolean current
) {}

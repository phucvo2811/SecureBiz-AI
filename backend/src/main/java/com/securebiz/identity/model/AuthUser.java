package com.securebiz.identity.model;

import java.time.Instant;
import java.util.UUID;

public record AuthUser(
        UUID id,
        String username,
        String email,
        String passwordHash,
        String status,
        int failedLoginCount,
        Instant lockedUntil,
        long authorizationVersion
) {}

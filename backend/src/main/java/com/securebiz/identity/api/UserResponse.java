package com.securebiz.identity.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id, String username, String email, String status,
        String accountType,
        int failedLoginCount, Instant lockedUntil, Instant lastLoginAt,
        long authorizationVersion, long version, Set<String> roles,
        Set<String> directPermissions, Set<String> effectivePermissions, long activeSessionCount,
        Instant createdAt, Instant updatedAt
) {}

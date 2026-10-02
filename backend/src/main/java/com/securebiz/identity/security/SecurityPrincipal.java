package com.securebiz.identity.security;

import java.util.Set;
import java.util.UUID;

public record SecurityPrincipal(
        UUID userId,
        UUID sessionId,
        String username,
        long authorizationVersion,
        Set<String> permissions
) {}

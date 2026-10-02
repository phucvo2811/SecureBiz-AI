package com.securebiz.identity.api;

import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id, String code, String name, String description,
        boolean systemRole, String status, Set<String> permissions
) {}

package com.securebiz.organization.api;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String code,
        String name,
        String description,
        UUID parentId,
        UUID managerId,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}

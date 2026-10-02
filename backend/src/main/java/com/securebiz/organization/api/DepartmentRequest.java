package com.securebiz.organization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DepartmentRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        UUID parentId,
        UUID managerId,
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status,
        Long version
) {}

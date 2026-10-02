package com.securebiz.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 200) String password,
        @NotBlank @Pattern(regexp = "PENDING|ACTIVE|LOCKED|DISABLED") String status,
        @Pattern(regexp = "OFFICIAL|TRAINEE|IT|MANAGER|OTHER") String accountType,
        Set<UUID> roleIds,
        Set<UUID> permissionIds,
        @Size(max=1000) String reason
) {}

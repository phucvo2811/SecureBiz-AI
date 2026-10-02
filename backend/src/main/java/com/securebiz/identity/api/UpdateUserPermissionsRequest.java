package com.securebiz.identity.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
public record UpdateUserPermissionsRequest(@NotNull Set<UUID> permissionIds,
                                           @NotBlank @Size(max=1000) String reason) {}

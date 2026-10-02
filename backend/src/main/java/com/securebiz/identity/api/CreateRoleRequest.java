package com.securebiz.identity.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
public record CreateRoleRequest(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{2,79}") String code,
                                @NotBlank @Size(max=150) String name,
                                @Size(max=500) String description,
                                Set<UUID> permissionIds) {}

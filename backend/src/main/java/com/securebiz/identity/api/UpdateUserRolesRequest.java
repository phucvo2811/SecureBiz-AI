package com.securebiz.identity.api;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record UpdateUserRolesRequest(@NotNull Set<UUID> roleIds) {}

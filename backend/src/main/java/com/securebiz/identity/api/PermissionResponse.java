package com.securebiz.identity.api;

import java.util.UUID;

public record PermissionResponse(UUID id, String code, String name, String description, String module,
                                 String status, boolean sensitive) {}

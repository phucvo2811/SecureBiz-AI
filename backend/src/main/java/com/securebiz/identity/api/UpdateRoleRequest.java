package com.securebiz.identity.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record UpdateRoleRequest(@NotBlank @Size(max=150) String name,@Size(max=500) String description) {}

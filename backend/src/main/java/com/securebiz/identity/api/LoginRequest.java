package com.securebiz.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254) String identifier,
        @NotBlank @Size(max = 200) String password,
        @Size(max = 200) String deviceName,
        @Size(max = 500) String deviceId
) {}

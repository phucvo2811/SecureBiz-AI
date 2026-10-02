package com.securebiz.organization.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Za-z0-9_-]+") String employeeCode,
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phone,
        @Email @Size(max = 254) String email,
        @NotNull UUID departmentId,
        @NotBlank @Size(max = 120) String position,
        @NotNull @PastOrPresent LocalDate hireDate,
        LocalDate terminationDate,
        @NotBlank @Pattern(regexp = "ACTIVE|ON_LEAVE|TERMINATED") String status,
        UUID userId,
        Long version
) {}

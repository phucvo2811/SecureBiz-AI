package com.securebiz.organization.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String employeeCode,
        String fullName,
        String phone,
        String email,
        UUID departmentId,
        String departmentName,
        String position,
        LocalDate hireDate,
        LocalDate terminationDate,
        String status,
        UUID userId,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}

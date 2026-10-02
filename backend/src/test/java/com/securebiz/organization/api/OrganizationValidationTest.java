package com.securebiz.organization.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationValidationTest {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() { factory.close(); }

    @Test
    void departmentRejectsUnsafeCodeAndUnknownStatus() {
        DepartmentRequest request = new DepartmentRequest(
                "Sales Department!", "Sales", null, null, null, "DELETED", null);
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("code", "status");
    }

    @Test
    void employeeRequiresValidEmailAndNonFutureHireDate() {
        EmployeeRequest request = new EmployeeRequest(
                "NV001", "Employee", null, "invalid-email", UUID.randomUUID(), "Sales",
                LocalDate.now().plusDays(1), null, "ACTIVE", null, null);
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("email", "hireDate");
    }
}

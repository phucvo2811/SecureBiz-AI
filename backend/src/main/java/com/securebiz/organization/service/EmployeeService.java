package com.securebiz.organization.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.organization.api.EmployeeRequest;
import com.securebiz.organization.api.EmployeeResponse;
import com.securebiz.organization.repository.DepartmentRepository;
import com.securebiz.organization.repository.EmployeeRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EmployeeService {
    private final EmployeeRepository repository;
    private final DepartmentRepository departments;
    private final ActivityLogRepository activityLogs;
    private final ObjectMapper objectMapper;

    public EmployeeService(EmployeeRepository repository, DepartmentRepository departments,
                           ActivityLogRepository activityLogs, ObjectMapper objectMapper) {
        this.repository = repository; this.departments = departments;
        this.activityLogs = activityLogs; this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasAuthority('employee.view')")
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> list(String search, UUID departmentId, String status, int page, int size) {
        String q = search == null ? "" : search.strip();
        String state = status == null ? "" : status.strip().toUpperCase();
        return PageResponse.of(repository.findPage(q, departmentId, state, size, (long) page * size),
                page, size, repository.count(q, departmentId, state));
    }

    @PreAuthorize("hasAuthority('employee.view')")
    @Transactional(readOnly = true)
    public EmployeeResponse get(UUID id) { return repository.findById(id).orElseThrow(() -> notFound(id)); }

    @PreAuthorize("hasAuthority('employee.manage')")
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        validateBusinessDates(request);
        requireDepartment(request.departmentId());
        UUID id = UUID.randomUUID(); UUID actor = CurrentActor.userId();
        try {
            EmployeeResponse created = repository.create(id, request, actor);
            activityLogs.appendMutation(actor, "EMPLOYEE_CREATE", "organization", "employee",
                    id, "null", json(created));
            return created;
        } catch (DataIntegrityViolationException exception) { throw conflict(); }
    }

    @PreAuthorize("hasAuthority('employee.manage')")
    @Transactional
    public EmployeeResponse update(UUID id, EmployeeRequest request) {
        if (request.version() == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "VERSION_REQUIRED", "Version is required for update");
        validateBusinessDates(request); requireDepartment(request.departmentId());
        EmployeeResponse before = repository.findById(id).orElseThrow(() -> notFound(id));
        UUID actor = CurrentActor.userId();
        try {
            if (repository.update(id, request, actor, request.version()) == 0) {
                throw new BusinessException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Employee was modified by another request");
            }
            EmployeeResponse updated = repository.findById(id).orElseThrow(() -> notFound(id));
            activityLogs.appendMutation(actor, "EMPLOYEE_UPDATE", "organization", "employee", id, json(before), json(updated));
            return updated;
        } catch (DataIntegrityViolationException exception) { throw conflict(); }
    }

    private void validateBusinessDates(EmployeeRequest request) {
        if (request.terminationDate() != null && request.terminationDate().isBefore(request.hireDate())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EMPLOYMENT_DATES", "Termination date cannot precede hire date");
        }
        if ("TERMINATED".equals(request.status()) && request.terminationDate() == null) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TERMINATION_DATE_REQUIRED", "Terminated employee requires a termination date");
        }
    }

    private void requireDepartment(UUID id) {
        if (departments.findById(id).isEmpty()) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DEPARTMENT", "Department does not exist");
    }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize audit snapshot", e); } }
    private BusinessException notFound(UUID id) { return new BusinessException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "Employee not found: " + id); }
    private BusinessException conflict() { return new BusinessException(HttpStatus.CONFLICT, "EMPLOYEE_CONFLICT", "Employee code, account, or department is invalid"); }
}

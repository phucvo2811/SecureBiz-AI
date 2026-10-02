package com.securebiz.organization.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.organization.api.DepartmentRequest;
import com.securebiz.organization.api.DepartmentResponse;
import com.securebiz.organization.repository.DepartmentRepository;
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
public class DepartmentService {
    private final DepartmentRepository repository;
    private final ActivityLogRepository activityLogs;
    private final ObjectMapper objectMapper;

    public DepartmentService(DepartmentRepository repository, ActivityLogRepository activityLogs,
                             ObjectMapper objectMapper) {
        this.repository = repository;
        this.activityLogs = activityLogs;
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasAuthority('department.view')")
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> list(String search, int page, int size) {
        String normalized = search == null ? "" : search.strip();
        return PageResponse.of(repository.findPage(normalized, size, (long) page * size), page, size,
                repository.count(normalized));
    }

    @PreAuthorize("hasAuthority('department.view')")
    @Transactional(readOnly = true)
    public DepartmentResponse get(UUID id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @PreAuthorize("hasAuthority('department.manage')")
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        UUID id = UUID.randomUUID();
        UUID actor = CurrentActor.userId();
        try {
            DepartmentResponse created = repository.create(id, request, actor);
            activityLogs.appendMutation(actor, "DEPARTMENT_CREATE", "organization", "department",
                    id, "null", json(created));
            return created;
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "DEPARTMENT_CONFLICT",
                    "Department code, parent, or manager is invalid");
        }
    }

    @PreAuthorize("hasAuthority('department.manage')")
    @Transactional
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        if (request.version() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VERSION_REQUIRED", "Version is required for update");
        }
        DepartmentResponse before = repository.findById(id).orElseThrow(() -> notFound(id));
        if (repository.parentWouldCreateCycle(id, request.parentId())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "DEPARTMENT_CYCLE",
                    "Department hierarchy cannot contain a cycle");
        }
        UUID actor = CurrentActor.userId();
        try {
            if (repository.update(id, request, actor, request.version()) == 0) {
                throw new BusinessException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT",
                        "Department was modified by another request");
            }
            DepartmentResponse updated = repository.findById(id).orElseThrow(() -> notFound(id));
            activityLogs.appendMutation(actor, "DEPARTMENT_UPDATE", "organization", "department",
                    id, json(before), json(updated));
            return updated;
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "DEPARTMENT_CONFLICT",
                    "Department code, parent, or manager is invalid");
        }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot serialize audit snapshot", exception); }
    }

    private BusinessException notFound(UUID id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "Department not found: " + id);
    }
}

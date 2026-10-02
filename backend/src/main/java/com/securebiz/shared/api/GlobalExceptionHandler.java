package com.securebiz.shared.api;

import com.securebiz.identity.api.AuthException;
import com.securebiz.shared.config.CorrelationIdFilter;
import com.securebiz.identity.security.SecurityPrincipal;
import com.securebiz.securitydata.service.DeniedAccessRecorder;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final DeniedAccessRecorder deniedAccessRecorder;

    public GlobalExceptionHandler(DeniedAccessRecorder deniedAccessRecorder) {
        this.deniedAccessRecorder = deniedAccessRecorder;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException exception, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), exception.status().value(), exception.code(),
                exception.getMessage(), correlationId(request), List.of());
        return ResponseEntity.status(exception.status()).body(body);
    }

    @ExceptionHandler(AuthException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthException exception, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), exception.status().value(), exception.code(),
                exception.getMessage(), correlationId(request), List.of());
        return ResponseEntity.status(exception.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiError.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(
                        error.getField(),
                        error.getCode() == null ? "INVALID" : error.getCode(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();

        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                correlationId(request),
                errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception,
                                                        HttpServletRequest request) {
        List<ApiError.FieldError> errors = exception.getConstraintViolations().stream()
                .map(error -> new ApiError.FieldError(error.getPropertyPath().toString(),
                        "INVALID", error.getMessage())).toList();
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "VALIDATION_ERROR",
                "Request validation failed", correlationId(request), errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMalformedBody(HttpMessageNotReadableException exception,
                                                  HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(Instant.now(), 400, "MALFORMED_JSON",
                "Request body is malformed", correlationId(request), List.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            deniedAccessRecorder.record(principal.userId(), principal.sessionId(), request.getRequestURI(),
                    request.getMethod(), request.getRemoteAddr(), request.getHeader("User-Agent"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(Instant.now(), 403,
                "ACCESS_DENIED", "You do not have permission to perform this action",
                correlationId(request), List.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        String correlationId = correlationId(request);
        log.error("Unhandled request failure correlationId={}", correlationId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(Instant.now(), 500,
                "INTERNAL_ERROR", "An unexpected error occurred", correlationId, List.of()));
    }

    private static String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return value == null ? null : value.toString();
    }
}

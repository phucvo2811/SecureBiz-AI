package com.securebiz.identity.service;

import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.identity.config.AuthProperties;
import com.securebiz.identity.repository.AuthRepository;
import com.securebiz.securitydata.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthAttemptRecorder {
    private final AuthRepository authRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AuthProperties properties;
    private final SecurityEventRepository securityEvents;

    public AuthAttemptRecorder(AuthRepository authRepository, ActivityLogRepository activityLogRepository,
                               AuthProperties properties, SecurityEventRepository securityEvents) {
        this.authRepository = authRepository;
        this.activityLogRepository = activityLogRepository;
        this.properties = properties;
        this.securityEvents = securityEvents;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID userId, String ipAddress, String userAgent, String reasonCode) {
        if (userId != null) {
            authRepository.registerFailure(userId, properties.maxFailedLogins(),
                    Instant.now().plus(properties.lockDuration()));
            if (authRepository.failedLoginCount(userId) >= properties.maxFailedLogins()) {
                long bucket = Instant.now().getEpochSecond() / Math.max(60, properties.lockDuration().toSeconds());
                securityEvents.create(userId, "FAILED_LOGIN_BURST", "identity", "HIGH",
                        "[\"failed_login_threshold\",\"temporary_lock\"]",
                        "{\"threshold\":" + properties.maxFailedLogins() + "}",
                        "failed-login-burst:" + userId + ":" + bucket);
            }
        }
        activityLogRepository.append(userId, null, "LOGIN_FAILED", "FAILURE", ipAddress, userAgent,
                "{\"reasonCode\":\"" + safeReason(reasonCode) + "\"}");
    }

    private static String safeReason(String reason) {
        return reason.replaceAll("[^A-Z0-9_]", "_");
    }
}

package com.securebiz.identity.service;

import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.identity.api.AuthException;
import com.securebiz.identity.api.AuthResponse;
import com.securebiz.identity.api.AuthSessionResponse;
import com.securebiz.identity.api.LoginRequest;
import com.securebiz.identity.config.AuthProperties;
import com.securebiz.identity.model.AuthUser;
import com.securebiz.identity.model.StoredRefreshToken;
import com.securebiz.identity.repository.AuthRepository;
import com.securebiz.identity.security.JwtService;
import com.securebiz.securitydata.repository.SecurityEventRepository;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final AuthRepository repository;
    private final ActivityLogRepository activityLogs;
    private final AuthAttemptRecorder attemptRecorder;
    private final LoginRateLimiter rateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final SecurityEventRepository securityEvents;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthRepository repository, ActivityLogRepository activityLogs,
                       AuthAttemptRecorder attemptRecorder, LoginRateLimiter rateLimiter,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuthProperties properties,
                       SecurityEventRepository securityEvents) {
        this.repository = repository;
        this.activityLogs = activityLogs;
        this.attemptRecorder = attemptRecorder;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.securityEvents = securityEvents;
    }

    @Transactional
    public AuthResult login(LoginRequest request, String ipAddress, String userAgent) {
        String identifier = request.identifier().strip().toLowerCase(Locale.ROOT);
        rateLimiter.check(identifier, ipAddress);
        AuthUser user = repository.findByIdentifier(identifier).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            attemptRecorder.failed(null, ipAddress, userAgent, "INVALID_CREDENTIALS");
            throw invalidCredentials();
        }
        Instant now = Instant.now();
        if ("DISABLED".equals(user.status()) || "PENDING".equals(user.status())) {
            attemptRecorder.failed(user.id(), ipAddress, userAgent, "ACCOUNT_UNAVAILABLE");
            throw invalidCredentials();
        }
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(now)) {
            attemptRecorder.failed(user.id(), ipAddress, userAgent, "ACCOUNT_LOCKED");
            throw new AuthException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is temporarily locked");
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            attemptRecorder.failed(user.id(), ipAddress, userAgent, "INVALID_CREDENTIALS");
            throw invalidCredentials();
        }

        repository.registerSuccess(user.id(), now);
        Set<String> permissions = repository.findActivePermissions(user.id(), now);
        Instant refreshExpiry = now.plus(properties.refreshTokenTtl());
        String deviceHash = hashNullable(request.deviceId());
        boolean newDevice = deviceHash != null && !repository.hasKnownDevice(user.id(), deviceHash);
        boolean newIp = ipAddress != null && !repository.hasKnownIp(user.id(), ipAddress);
        UUID sessionId = repository.createSession(user.id(), deviceHash,
                request.deviceName(), ipAddress, userAgent, refreshExpiry);
        String refreshToken = randomToken();
        repository.createRefreshToken(sessionId, UUID.randomUUID(), sha256(refreshToken), refreshExpiry);
        JwtService.IssuedAccessToken access = jwtService.issue(user.id(), sessionId, user.username(),
                user.authorizationVersion(), permissions);
        activityLogs.append(user.id(), sessionId, "LOGIN_SUCCESS", "SUCCESS", ipAddress, userAgent, "{}");
        if (newDevice) {
            securityEvents.create(user.id(), "NEW_DEVICE", "identity", "MEDIUM",
                    "[\"first_seen_device\"]", "{\"sessionId\":\"" + sessionId + "\"}",
                    "new-device:" + user.id() + ":" + deviceHash);
        }
        if (newIp) {
            securityEvents.create(user.id(), "NEW_IP", "identity", "MEDIUM",
                    "[\"first_seen_ip\"]", "{\"sessionId\":\"" + sessionId + "\"}",
                    "new-ip:" + user.id() + ":" + sha256(ipAddress));
        }
        return result(user, permissions, access, refreshToken);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthResult refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalidRefresh();
        }
        StoredRefreshToken stored = repository.lockRefreshToken(sha256(rawRefreshToken)).orElseThrow(this::invalidRefresh);
        Instant now = Instant.now();
        if (stored.usedAt() != null || stored.revokedAt() != null) {
            repository.revokeFamily(stored.familyId(), now);
            repository.revokeSession(stored.sessionId(), now, "REFRESH_TOKEN_REUSE");
            activityLogs.append(stored.userId(), stored.sessionId(), "REFRESH_TOKEN_REUSE", "DENIED",
                    ipAddress, userAgent, "{}");
            throw new AuthException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Session has been revoked");
        }
        if (!stored.expiresAt().isAfter(now)) {
            repository.revokeFamily(stored.familyId(), now);
            throw invalidRefresh();
        }
        AuthUser user = repository.findById(stored.userId()).orElseThrow(this::invalidRefresh);
        if (!"ACTIVE".equals(user.status())) {
            repository.revokeSession(stored.sessionId(), now, "ACCOUNT_UNAVAILABLE");
            throw invalidRefresh();
        }

        String newRawToken = randomToken();
        UUID newId = repository.createRefreshToken(stored.sessionId(), stored.familyId(),
                sha256(newRawToken), now.plus(properties.refreshTokenTtl()));
        repository.rotateRefreshToken(stored.id(), newId, now);
        repository.touchSession(stored.sessionId(), now, ipAddress, userAgent);
        Set<String> permissions = repository.findActivePermissions(user.id(), now);
        JwtService.IssuedAccessToken access = jwtService.issue(user.id(), stored.sessionId(), user.username(),
                user.authorizationVersion(), permissions);
        return result(user, permissions, access, newRawToken);
    }

    @Transactional
    public void logout(String rawRefreshToken, String ipAddress, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        repository.lockRefreshToken(sha256(rawRefreshToken)).ifPresent(token -> {
            Instant now = Instant.now();
            repository.revokeSession(token.sessionId(), now, "USER_LOGOUT");
            activityLogs.append(token.userId(), token.sessionId(), "LOGOUT", "SUCCESS", ipAddress, userAgent, "{}");
        });
    }

    @Transactional(readOnly = true)
    public List<AuthSessionResponse> sessions() {
        return repository.sessions(CurrentActor.userId(), CurrentActor.sessionId());
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        UUID actor = CurrentActor.userId();
        if (!repository.ownsSession(actor, sessionId)) {
            throw new AuthException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found");
        }
        Instant now = Instant.now();
        repository.revokeSession(sessionId, now, "USER_REVOKED");
        activityLogs.append(actor, CurrentActor.sessionId(), "SESSION_REVOKE", "SUCCESS", null, null,
                "{\"revokedSessionId\":\"" + sessionId + "\"}");
    }

    private AuthResult result(AuthUser user, Set<String> permissions, JwtService.IssuedAccessToken access,
                              String refreshToken) {
        AuthResponse response = new AuthResponse(access.value(), "Bearer", access.expiresAt(),
                new AuthResponse.CurrentUser(user.id(), user.username(), user.email(), permissions));
        return new AuthResult(response, refreshToken, Instant.now().plus(properties.refreshTokenTtl()));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashNullable(String value) {
        return value == null || value.isBlank() ? null : sha256(value);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }

    private AuthException invalidRefresh() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid");
    }

    public record AuthResult(AuthResponse response, String refreshToken, Instant refreshExpiresAt) {}
}

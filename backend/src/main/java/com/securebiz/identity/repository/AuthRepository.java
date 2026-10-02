package com.securebiz.identity.repository;

import com.securebiz.identity.model.AuthUser;
import com.securebiz.identity.model.StoredRefreshToken;
import com.securebiz.identity.api.AuthSessionResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class AuthRepository {
    private final JdbcClient jdbc;

    public AuthRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AuthUser> findByIdentifier(String identifier) {
        return jdbc.sql("""
                SELECT id, username, email, password_hash, status, failed_login_count,
                       locked_until, authorization_version
                FROM users
                WHERE username = :identifier OR email = :identifier
                """)
                .param("identifier", identifier)
                .query((rs, ignored) -> new AuthUser(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("status"),
                        rs.getInt("failed_login_count"),
                        rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant(),
                        rs.getLong("authorization_version")))
                .optional();
    }

    public Optional<AuthUser> findById(UUID userId) {
        return jdbc.sql("""
                SELECT id, username, email, password_hash, status, failed_login_count,
                       locked_until, authorization_version
                FROM users WHERE id = :userId
                """)
                .param("userId", userId)
                .query((rs, ignored) -> new AuthUser(
                        rs.getObject("id", UUID.class), rs.getString("username"), rs.getString("email"),
                        rs.getString("password_hash"), rs.getString("status"), rs.getInt("failed_login_count"),
                        rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toInstant(),
                        rs.getLong("authorization_version")))
                .optional();
    }

    public Set<String> findActivePermissions(UUID userId, Instant now) {
        return new HashSet<>(jdbc.sql("""
                SELECT DISTINCT code FROM (
                  SELECT p.code FROM user_roles ur
                  JOIN roles r ON r.id=ur.role_id AND r.status='ACTIVE'
                  JOIN role_permissions rp ON rp.role_id=r.id
                  JOIN permissions p ON p.id=rp.permission_id AND p.status='ACTIVE'
                  WHERE ur.user_id=:userId AND ur.valid_from<=:now
                    AND (ur.valid_until IS NULL OR ur.valid_until>:now)
                  UNION
                  SELECT p.code FROM user_permission_grants upg
                  JOIN permissions p ON p.id=upg.permission_id AND p.status='ACTIVE'
                  WHERE upg.user_id=:userId
                ) effective_permissions
                """)
                .param("userId", userId)
                .param("now", Timestamp.from(now))
                .query(String.class)
                .list());
    }

    public boolean hasKnownDevice(UUID userId, String deviceIdHash) {
        if (deviceIdHash == null) return true;
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM auth_sessions WHERE user_id=:user AND device_id_hash=:device)")
                .param("user", userId).param("device", deviceIdHash).query(Boolean.class).single();
    }

    public boolean hasKnownIp(UUID userId, String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) return true;
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM auth_sessions WHERE user_id=:user AND ip_address=CAST(:ip AS inet))")
                .param("user", userId).param("ip", ipAddress).query(Boolean.class).single();
    }

    public int failedLoginCount(UUID userId) {
        return jdbc.sql("SELECT failed_login_count FROM users WHERE id=:id")
                .param("id", userId).query(Integer.class).single();
    }

    public List<AuthSessionResponse> sessions(UUID userId, UUID currentSessionId) {
        return jdbc.sql("""
                SELECT id, device_name, host(ip_address) AS ip_address, user_agent,
                       started_at, last_seen_at, expires_at, revoked_at, revoke_reason
                FROM auth_sessions
                WHERE user_id = :userId
                ORDER BY started_at DESC
                LIMIT 100
                """)
                .param("userId", userId)
                .query((rs, ignored) -> new AuthSessionResponse(
                        rs.getObject("id", UUID.class), rs.getString("device_name"),
                        rs.getString("ip_address"), rs.getString("user_agent"),
                        rs.getTimestamp("started_at").toInstant(),
                        rs.getTimestamp("last_seen_at").toInstant(),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toInstant(),
                        rs.getString("revoke_reason"), rs.getObject("id", UUID.class).equals(currentSessionId)))
                .list();
    }

    public boolean ownsSession(UUID userId, UUID sessionId) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM auth_sessions WHERE id=:sessionId AND user_id=:userId)")
                .param("sessionId", sessionId).param("userId", userId).query(Boolean.class).single();
    }

    public boolean tokenStateIsActive(UUID userId, UUID sessionId, long authorizationVersion) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM users u
                    JOIN auth_sessions s ON s.user_id = u.id
                    WHERE u.id = :userId AND u.status = 'ACTIVE'
                      AND u.authorization_version = :authorizationVersion
                      AND s.id = :sessionId AND s.revoked_at IS NULL AND s.expires_at > now()
                )
                """)
                .param("userId", userId)
                .param("sessionId", sessionId)
                .param("authorizationVersion", authorizationVersion)
                .query(Boolean.class).single();
    }

    public void registerFailure(UUID userId, int maxAttempts, Instant lockedUntil) {
        jdbc.sql("""
                UPDATE users
                SET failed_login_count = failed_login_count + 1,
                    locked_until = CASE WHEN failed_login_count + 1 >= :maxAttempts
                                        THEN :lockedUntil ELSE locked_until END,
                    status = CASE WHEN failed_login_count + 1 >= :maxAttempts
                                  THEN 'LOCKED' ELSE status END,
                    updated_at = now(), version = version + 1
                WHERE id = :userId
                """)
                .param("maxAttempts", maxAttempts)
                .param("lockedUntil", Timestamp.from(lockedUntil))
                .param("userId", userId)
                .update();
    }

    public void registerSuccess(UUID userId, Instant now) {
        jdbc.sql("""
                UPDATE users
                SET failed_login_count = 0, locked_until = NULL, status = 'ACTIVE',
                    last_login_at = :now, updated_at = :now, version = version + 1
                WHERE id = :userId
                """)
                .param("userId", userId)
                .param("now", Timestamp.from(now))
                .update();
    }

    public UUID createSession(UUID userId, String deviceIdHash, String deviceName,
                              String ipAddress, String userAgent, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO auth_sessions
                    (id, user_id, device_id_hash, device_name, ip_address, user_agent, expires_at)
                VALUES
                    (:id, :userId, :deviceIdHash, :deviceName, CAST(:ipAddress AS inet), :userAgent, :expiresAt)
                """)
                .param("id", id)
                .param("userId", userId)
                .param("deviceIdHash", deviceIdHash)
                .param("deviceName", deviceName)
                .param("ipAddress", ipAddress)
                .param("userAgent", userAgent)
                .param("expiresAt", Timestamp.from(expiresAt))
                .update();
        return id;
    }

    public UUID createRefreshToken(UUID sessionId, UUID familyId, String tokenHash, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO refresh_tokens (id, session_id, token_family_id, token_hash, expires_at)
                VALUES (:id, :sessionId, :familyId, :tokenHash, :expiresAt)
                """)
                .param("id", id)
                .param("sessionId", sessionId)
                .param("familyId", familyId)
                .param("tokenHash", tokenHash)
                .param("expiresAt", Timestamp.from(expiresAt))
                .update();
        return id;
    }

    public Optional<StoredRefreshToken> lockRefreshToken(String tokenHash) {
        return jdbc.sql("""
                SELECT rt.id, rt.session_id, s.user_id, rt.token_family_id, rt.expires_at,
                       rt.used_at, rt.revoked_at
                FROM refresh_tokens rt
                JOIN auth_sessions s ON s.id = rt.session_id
                WHERE rt.token_hash = :tokenHash
                  AND s.revoked_at IS NULL
                  AND s.expires_at > now()
                FOR UPDATE OF rt, s
                """)
                .param("tokenHash", tokenHash)
                .query((rs, ignored) -> new StoredRefreshToken(
                        rs.getObject("id", UUID.class),
                        rs.getObject("session_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("token_family_id", UUID.class),
                        rs.getTimestamp("expires_at").toInstant(),
                        rs.getTimestamp("used_at") == null ? null : rs.getTimestamp("used_at").toInstant(),
                        rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toInstant()))
                .optional();
    }

    public void rotateRefreshToken(UUID oldId, UUID newId, Instant usedAt) {
        jdbc.sql("""
                UPDATE refresh_tokens SET used_at = :usedAt, replaced_by_token_id = :newId
                WHERE id = :oldId AND used_at IS NULL AND revoked_at IS NULL
                """)
                .param("usedAt", Timestamp.from(usedAt))
                .param("newId", newId)
                .param("oldId", oldId)
                .update();
    }

    public void touchSession(UUID sessionId, Instant seenAt, String ipAddress, String userAgent) {
        jdbc.sql("""
                UPDATE auth_sessions
                SET last_seen_at=:seenAt,
                    ip_address=COALESCE(CAST(:ipAddress AS inet),ip_address),
                    user_agent=COALESCE(:userAgent,user_agent)
                WHERE id=:sessionId AND revoked_at IS NULL
                """)
                .param("seenAt", Timestamp.from(seenAt)).param("ipAddress", ipAddress)
                .param("userAgent", userAgent).param("sessionId", sessionId).update();
    }

    public void revokeFamily(UUID familyId, Instant now) {
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = COALESCE(revoked_at, :now) WHERE token_family_id = :familyId")
                .param("now", Timestamp.from(now))
                .param("familyId", familyId)
                .update();
    }

    public void revokeSession(UUID sessionId, Instant now, String reason) {
        jdbc.sql("""
                UPDATE auth_sessions SET revoked_at = COALESCE(revoked_at, :now), revoke_reason = :reason
                WHERE id = :sessionId
                """)
                .param("now", Timestamp.from(now))
                .param("reason", reason)
                .param("sessionId", sessionId)
                .update();
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = COALESCE(revoked_at, :now) WHERE session_id = :sessionId")
                .param("now", Timestamp.from(now))
                .param("sessionId", sessionId)
                .update();
    }
}

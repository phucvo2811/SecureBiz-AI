package com.securebiz.identity.repository;

import com.securebiz.identity.api.PermissionResponse;
import com.securebiz.identity.api.RoleResponse;
import com.securebiz.identity.api.UserResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class IdentityAdminRepository {
    private final JdbcClient jdbc;

    public IdentityAdminRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<UserResponse> findUsers(String search, int size, long offset) {
        return jdbc.sql("""
                SELECT id, username, email, status, account_type, failed_login_count, locked_until, last_login_at,
                       authorization_version, version, created_at, updated_at
                FROM users WHERE :search='' OR username ILIKE :pattern OR email ILIKE :pattern
                ORDER BY created_at DESC, id LIMIT :size OFFSET :offset
                """).param("search", search).param("pattern", "%" + search + "%")
                .param("size", size).param("offset", offset).query(this::mapUser).list();
    }

    public long countUsers(String search) {
        return jdbc.sql("SELECT count(*) FROM users WHERE :search='' OR username ILIKE :pattern OR email ILIKE :pattern")
                .param("search", search).param("pattern", "%" + search + "%").query(Long.class).single();
    }

    public Optional<UserResponse> findUser(UUID id) {
        return jdbc.sql("""
                SELECT id, username, email, status, account_type, failed_login_count, locked_until, last_login_at,
                       authorization_version, version, created_at, updated_at FROM users WHERE id=:id
                """).param("id", id).query(this::mapUser).optional();
    }

    public UUID createUser(String username, String email, String passwordHash, String status, String accountType) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO users (id,username,email,password_hash,status,account_type) VALUES (:id,:username,:email,:hash,:status,:type)")
                .param("id", id).param("username", username).param("email", email)
                .param("hash", passwordHash).param("status", status).param("type",accountType).update();
        return id;
    }

    public void replaceUserRoles(UUID userId, Set<UUID> roleIds, UUID actor) {
        jdbc.sql("DELETE FROM user_roles WHERE user_id=:userId").param("userId", userId).update();
        for (UUID roleId : roleIds) {
            jdbc.sql("INSERT INTO user_roles (user_id,role_id,assigned_by) VALUES (:userId,:roleId,:actor)")
                    .param("userId", userId).param("roleId", roleId).param("actor", actor).update();
        }
        jdbc.sql("UPDATE users SET authorization_version=authorization_version+1, updated_at=now(), version=version+1 WHERE id=:id")
                .param("id", userId).update();
    }

    public boolean userHasRole(UUID userId, String roleCode) {
        return jdbc.sql("""
                SELECT EXISTS(SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id
                              WHERE ur.user_id=:userId AND r.code=:code)
                """).param("userId", userId).param("code", roleCode).query(Boolean.class).single();
    }

    public boolean roleIdsContainCode(Set<UUID> roleIds, String roleCode) {
        if (roleIds.isEmpty()) return false;
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM roles WHERE id IN (:ids) AND code=:code)")
                .param("ids", roleIds).param("code", roleCode).query(Boolean.class).single();
    }

    public boolean validActiveRoleIds(Set<UUID> ids) {
        if(ids.isEmpty()) return true;
        return jdbc.sql("SELECT count(*)=:count FROM roles WHERE id IN (:ids) AND status='ACTIVE' AND code<>'ADMIN'")
                .param("count",ids.size()).param("ids",ids).query(Boolean.class).single();
    }
    public boolean rolesContainBusinessMutation(Set<UUID> ids){if(ids.isEmpty())return false;return jdbc.sql("""
      SELECT EXISTS(SELECT 1 FROM roles r JOIN role_permissions rp ON rp.role_id=r.id JOIN permissions p ON p.id=rp.permission_id WHERE r.id IN(:ids) AND p.code IN('order.create','order.cancel','return.approve','inventory.adjust','inventory.transfer','inventory.stocktake','purchase.create','purchase.approve','purchase.receive','product.create','product.update','employee.manage','department.manage'))
      """).param("ids",ids).query(Boolean.class).single();}

    public boolean validActivePermissionIds(Set<UUID> ids) {
        if(ids.isEmpty()) return true;
        return jdbc.sql("SELECT count(*)=:count FROM permissions WHERE id IN (:ids) AND status='ACTIVE'")
                .param("count",ids.size()).param("ids",ids).query(Boolean.class).single();
    }

    public Set<String> directPermissions(UUID userId){return new HashSet<>(jdbc.sql("SELECT p.code FROM user_permission_grants g JOIN permissions p ON p.id=g.permission_id WHERE g.user_id=:id AND p.status='ACTIVE' ORDER BY p.code").param("id",userId).query(String.class).list());}
    public Set<String> effectivePermissions(UUID userId){return new HashSet<>(jdbc.sql("""
      SELECT p.code FROM permissions p WHERE p.status='ACTIVE' AND (EXISTS(SELECT 1 FROM user_permission_grants g WHERE g.user_id=:id AND g.permission_id=p.id) OR EXISTS(SELECT 1 FROM user_roles ur JOIN roles r ON r.id=ur.role_id AND r.status='ACTIVE' JOIN role_permissions rp ON rp.role_id=r.id WHERE ur.user_id=:id AND rp.permission_id=p.id AND ur.valid_from<=now() AND (ur.valid_until IS NULL OR ur.valid_until>now()))) ORDER BY p.code
      """).param("id",userId).query(String.class).list());}
    public void replaceUserPermissions(UUID userId,Set<UUID> ids,UUID actor,String reason){jdbc.sql("DELETE FROM user_permission_grants WHERE user_id=:id").param("id",userId).update();for(UUID permission:ids)jdbc.sql("INSERT INTO user_permission_grants(user_id,permission_id,granted_by,reason) VALUES(:user,:permission,:actor,:reason)").param("user",userId).param("permission",permission).param("actor",actor).param("reason",reason.strip()).update();bumpAuthorization(userId);}
    public void bumpAuthorization(UUID userId){jdbc.sql("UPDATE users SET authorization_version=authorization_version+1,updated_at=now(),version=version+1 WHERE id=:id").param("id",userId).update();}
    public void updateUser(UUID id,String email,String accountType){jdbc.sql("UPDATE users SET email=:email,account_type=:type,updated_at=now(),version=version+1 WHERE id=:id").param("id",id).param("email",email).param("type",accountType).update();}
    public void setUserStatus(UUID id,String status){jdbc.sql("UPDATE users SET status=:status,failed_login_count=CASE WHEN :status='ACTIVE' THEN 0 ELSE failed_login_count END,locked_until=CASE WHEN :status='ACTIVE' THEN NULL ELSE locked_until END,authorization_version=authorization_version+1,updated_at=now(),version=version+1 WHERE id=:id").param("id",id).param("status",status).update();}
    public long activeSessionCount(UUID id){return jdbc.sql("SELECT count(*) FROM auth_sessions WHERE user_id=:id AND revoked_at IS NULL AND expires_at>now()").param("id",id).query(Long.class).single();}
    public UUID createRole(String code,String name,String description){UUID id=UUID.randomUUID();jdbc.sql("INSERT INTO roles(id,code,name,description,system_role,status) VALUES(:id,:code,:name,:description,false,'ACTIVE')").param("id",id).param("code",code).param("name",name).param("description",description).update();return id;}
    public void updateRole(UUID id,String name,String description){jdbc.sql("UPDATE roles SET name=:name,description=:description,updated_at=now() WHERE id=:id AND system_role=false").param("id",id).param("name",name).param("description",description).update();}
    public void deactivateRole(UUID id){jdbc.sql("UPDATE roles SET status='INACTIVE',updated_at=now() WHERE id=:id AND system_role=false").param("id",id).update();jdbc.sql("UPDATE users SET authorization_version=authorization_version+1 WHERE id IN(SELECT user_id FROM user_roles WHERE role_id=:id)").param("id",id).update();}
    public long roleUserCount(UUID id){return jdbc.sql("SELECT count(*) FROM user_roles WHERE role_id=:id").param("id",id).query(Long.class).single();}

    public long countActiveOwners() {
        return jdbc.sql("""
                SELECT count(DISTINCT u.id) FROM users u
                JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id
                WHERE r.code='OWNER' AND u.status='ACTIVE'
                """).query(Long.class).single();
    }

    public List<RoleResponse> findRoles() {
        return jdbc.sql("SELECT id,code,name,description,system_role,status FROM roles ORDER BY code")
                .query((rs, ignored) -> new RoleResponse(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getString("description"), rs.getBoolean("system_role"),
                        rs.getString("status"), findPermissionCodes(rs.getObject("id", UUID.class)))).list();
    }

    public Optional<RoleResponse> findRole(UUID id) {
        return jdbc.sql("SELECT id,code,name,description,system_role,status FROM roles WHERE id=:id")
                .param("id", id).query((rs, ignored) -> new RoleResponse(rs.getObject("id", UUID.class),
                        rs.getString("code"), rs.getString("name"), rs.getString("description"),
                        rs.getBoolean("system_role"), rs.getString("status"), findPermissionCodes(id))).optional();
    }

    public List<PermissionResponse> findPermissions() {
        return jdbc.sql("SELECT id,code,name,description,module,status,sensitive FROM permissions ORDER BY module,code")
                .query((rs, ignored) -> new PermissionResponse(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getString("description"), rs.getString("module"),rs.getString("status"),rs.getBoolean("sensitive"))).list();
    }

    public void replaceRolePermissions(UUID roleId, Set<UUID> permissionIds, UUID actor) {
        jdbc.sql("DELETE FROM role_permissions WHERE role_id=:roleId").param("roleId", roleId).update();
        for (UUID permissionId : permissionIds) {
            jdbc.sql("INSERT INTO role_permissions (role_id,permission_id,granted_by) VALUES (:roleId,:permissionId,:actor)")
                    .param("roleId", roleId).param("permissionId", permissionId).param("actor", actor).update();
        }
        jdbc.sql("""
                UPDATE users SET authorization_version=authorization_version+1, updated_at=now(), version=version+1
                WHERE id IN (SELECT user_id FROM user_roles WHERE role_id=:roleId)
                """).param("roleId", roleId).update();
    }

    public void revokeUserSessions(UUID userId, String reason) {
        jdbc.sql("UPDATE auth_sessions SET revoked_at=COALESCE(revoked_at,now()), revoke_reason=:reason WHERE user_id=:userId")
                .param("reason", reason).param("userId", userId).update();
        jdbc.sql("""
                UPDATE refresh_tokens SET revoked_at=COALESCE(revoked_at,now())
                WHERE session_id IN (SELECT id FROM auth_sessions WHERE user_id=:userId)
                """).param("userId", userId).update();
    }

    public void revokeSessionsForRole(UUID roleId) {
        jdbc.sql("""
                UPDATE auth_sessions SET revoked_at=COALESCE(revoked_at,now()), revoke_reason='ROLE_PERMISSION_CHANGE'
                WHERE user_id IN (SELECT user_id FROM user_roles WHERE role_id=:roleId)
                """).param("roleId", roleId).update();
    }

    private Set<String> findRoleCodes(UUID userId) {
        return new HashSet<>(jdbc.sql("SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id=r.id WHERE ur.user_id=:id ORDER BY r.code")
                .param("id", userId).query(String.class).list());
    }
    private Set<String> findPermissionCodes(UUID roleId) {
        return new HashSet<>(jdbc.sql("SELECT p.code FROM permissions p JOIN role_permissions rp ON rp.permission_id=p.id WHERE rp.role_id=:id ORDER BY p.code")
                .param("id", roleId).query(String.class).list());
    }
    private UserResponse mapUser(ResultSet rs, int ignored) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new UserResponse(id, rs.getString("username"), rs.getString("email"), rs.getString("status"),rs.getString("account_type"),
                rs.getInt("failed_login_count"), instant(rs, "locked_until"), instant(rs, "last_login_at"),
                rs.getLong("authorization_version"), rs.getLong("version"), findRoleCodes(id),
                directPermissions(id),effectivePermissions(id),activeSessionCount(id),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
    private static Instant instant(ResultSet rs, String name) throws SQLException {
        return rs.getTimestamp(name) == null ? null : rs.getTimestamp(name).toInstant();
    }
}

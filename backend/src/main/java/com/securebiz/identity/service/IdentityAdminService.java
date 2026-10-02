package com.securebiz.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebiz.audit.repository.ActivityLogRepository;
import com.securebiz.identity.api.CreateUserRequest;
import com.securebiz.identity.api.PermissionResponse;
import com.securebiz.identity.api.RoleResponse;
import com.securebiz.identity.api.UserResponse;
import com.securebiz.identity.api.CreateRoleRequest;
import com.securebiz.identity.api.UpdateRoleRequest;
import com.securebiz.identity.api.UpdateUserPermissionsRequest;
import com.securebiz.identity.api.UpdateUserRequest;
import com.securebiz.identity.repository.IdentityAdminRepository;
import com.securebiz.shared.api.BusinessException;
import com.securebiz.shared.api.PageResponse;
import com.securebiz.shared.security.CurrentActor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class IdentityAdminService {
    private final IdentityAdminRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogRepository activityLogs;
    private final ObjectMapper objectMapper;

    public IdentityAdminService(IdentityAdminRepository repository, PasswordEncoder passwordEncoder,
                                ActivityLogRepository activityLogs, ObjectMapper objectMapper) {
        this.repository = repository; this.passwordEncoder = passwordEncoder;
        this.activityLogs = activityLogs; this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasAuthority('user.view')")
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> users(String search, int page, int size) {
        String q = search == null ? "" : search.strip();
        return PageResponse.of(repository.findUsers(q, size, (long) page * size), page, size, repository.countUsers(q));
    }

    @PreAuthorize("hasAuthority('user.manage')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        UUID actor = CurrentActor.userId();
        Set<UUID> roles=request.roleIds()==null?Set.of():request.roleIds();
        Set<UUID> permissions=request.permissionIds()==null?Set.of():request.permissionIds();
        boolean owner=repository.userHasRole(actor,"OWNER");
        if(!repository.validActiveRoleIds(roles)) throw invalid("INVALID_ROLE_ASSIGNMENT","One or more roles are invalid or inactive");
        if(!repository.validActivePermissionIds(permissions)) throw invalid("INVALID_PERMISSION_ASSIGNMENT","One or more permissions are invalid or inactive");
        if((repository.roleIdsContainCode(roles,"OWNER")||repository.roleIdsContainCode(roles,"IT_ADMIN")||!permissions.isEmpty())&&!owner)
            throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only OWNER may assign privileged roles or direct permissions");
        if(!owner&&repository.rolesContainBusinessMutation(roles)) throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only OWNER may assign business-mutation roles");
        try {
            UUID id = repository.createUser(request.username().strip().toLowerCase(Locale.ROOT),
                    request.email().strip().toLowerCase(Locale.ROOT), passwordEncoder.encode(request.password()),
                    request.status(),request.accountType()==null?"OFFICIAL":request.accountType());
            repository.replaceUserRoles(id,roles, actor);
            if(!permissions.isEmpty()) repository.replaceUserPermissions(id,permissions,actor,requiredReason(request.reason()));
            UserResponse response = repository.findUser(id).orElseThrow();
            activityLogs.appendMutation(actor, "USER_CREATE", "identity", "user", id, "null", json(response));
            return response;
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "USER_CONFLICT", "Username, email, or role is invalid");
        }
    }

    @PreAuthorize("hasAuthority('role.view')")
    @Transactional(readOnly = true)
    public List<RoleResponse> roles() { return repository.findRoles(); }

    @PreAuthorize("hasAuthority('role.view')")
    @Transactional(readOnly = true)
    public List<PermissionResponse> permissions() { return repository.findPermissions(); }

    @PreAuthorize("hasAuthority('user.manage')")
    @Transactional
    public UserResponse replaceUserRoles(UUID userId, Set<UUID> roleIds) {
        UserResponse before = repository.findUser(userId).orElseThrow(() -> notFound("USER_NOT_FOUND", "User not found"));
        UUID actor=CurrentActor.userId(); boolean owner=repository.userHasRole(actor,"OWNER");
        if(!repository.validActiveRoleIds(roleIds)) throw invalid("INVALID_ROLE_ASSIGNMENT","One or more roles are invalid or inactive");
        if(!owner&&(actor.equals(userId)||before.roles().contains("OWNER")||roleIdsContainPrivileged(roleIds))) throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only OWNER may change OWNER/IT_ADMIN or own privileges");
        if(!owner&&repository.rolesContainBusinessMutation(roleIds)) throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only OWNER may assign business-mutation roles");
        if (repository.userHasRole(userId, "OWNER") && !repository.roleIdsContainCode(roleIds, "OWNER")
                && repository.countActiveOwners() <= 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "LAST_OWNER_REQUIRED", "The last active OWNER cannot be removed");
        }
        try {
            repository.replaceUserRoles(userId, roleIds, actor);
            repository.revokeUserSessions(userId, "USER_ROLE_CHANGE");
            UserResponse after = repository.findUser(userId).orElseThrow();
            activityLogs.appendMutation(actor, itAdminAction(before.roles(),after.roles()), "identity", "user", userId, json(before), json(after));
            return after;
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ROLE_ASSIGNMENT", "One or more roles are invalid");
        }
    }

    @PreAuthorize("hasAuthority('role.manage')")
    @Transactional
    public RoleResponse replaceRolePermissions(UUID roleId, Set<UUID> permissionIds) {
        RoleResponse before = repository.findRole(roleId).orElseThrow(() -> notFound("ROLE_NOT_FOUND", "Role not found"));
        UUID actor = CurrentActor.userId();
        if(!repository.userHasRole(actor,"OWNER")) throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only OWNER may change role permissions");
        if(!repository.validActivePermissionIds(permissionIds)) throw invalid("INVALID_PERMISSION_ASSIGNMENT","One or more permissions are invalid or inactive");
        try {
            repository.replaceRolePermissions(roleId, permissionIds, actor);
            repository.revokeSessionsForRole(roleId);
            RoleResponse after = repository.findRole(roleId).orElseThrow();
            activityLogs.appendMutation(actor, "ROLE_PERMISSION_REPLACE", "identity", "role", roleId, json(before), json(after));
            return after;
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PERMISSION_ASSIGNMENT", "One or more permissions are invalid");
        }
    }

    @PreAuthorize("hasAuthority('user.view')") @Transactional(readOnly=true)
    public UserResponse user(UUID id){return repository.findUser(id).orElseThrow(()->notFound("USER_NOT_FOUND","User not found"));}

    @PreAuthorize("hasAuthority('user.manage')") @Transactional
    public UserResponse updateUser(UUID id,UpdateUserRequest request){UserResponse before=user(id);UUID actor=CurrentActor.userId();protectOwnerTarget(actor,before);repository.updateUser(id,request.email().strip().toLowerCase(Locale.ROOT),request.accountType()==null?before.accountType():request.accountType());UserResponse after=user(id);activityLogs.appendMutation(actor,"USER_UPDATE","identity","user",id,json(before),json(after));return after;}

    @PreAuthorize("hasAuthority('user.permission.view')") @Transactional(readOnly=true)
    public UserResponse effectivePermissions(UUID id){return user(id);}

    @PreAuthorize("hasAuthority('user.permission.assign')") @Transactional
    public UserResponse replaceUserPermissions(UUID id,UpdateUserPermissionsRequest request){UUID actor=CurrentActor.userId();if(!repository.userHasRole(actor,"OWNER"))throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only OWNER may grant direct permissions");if(actor.equals(id))throw new BusinessException(HttpStatus.CONFLICT,"SELF_PRIVILEGE_CHANGE","Direct permissions cannot be changed for your own account");if(!repository.validActivePermissionIds(request.permissionIds()))throw invalid("INVALID_PERMISSION_ASSIGNMENT","One or more permissions are invalid or inactive");UserResponse before=user(id);repository.replaceUserPermissions(id,request.permissionIds(),actor,request.reason());repository.revokeUserSessions(id,"USER_PERMISSION_CHANGE");UserResponse after=user(id);activityLogs.appendMutation(actor,"USER_PERMISSION_REPLACE","identity","user",id,json(before),json(after));return after;}

    @PreAuthorize("hasAuthority('user.account.lock')") @Transactional public UserResponse lock(UUID id){return changeStatus(id,"LOCKED","USER_LOCK");}
    @PreAuthorize("hasAuthority('user.account.unlock')") @Transactional public UserResponse unlock(UUID id){return changeStatus(id,"ACTIVE","USER_UNLOCK");}
    @PreAuthorize("hasAuthority('session.revoke')") @Transactional public void revokeSessions(UUID id){UserResponse target=user(id);protectOwnerTarget(CurrentActor.userId(),target);repository.revokeUserSessions(id,"ADMIN_REVOKE");activityLogs.appendMutation(CurrentActor.userId(),"USER_SESSION_REVOKE","identity","user",id,"null","{\"sessionsRevoked\":true}");}

    @PreAuthorize("hasAuthority('role.manage')") @Transactional public RoleResponse createRole(CreateRoleRequest request){requireOwner();if(!repository.validActivePermissionIds(request.permissionIds()==null?Set.of():request.permissionIds()))throw invalid("INVALID_PERMISSION_ASSIGNMENT","Invalid permissions");UUID id=repository.createRole(request.code(),request.name().strip(),request.description());repository.replaceRolePermissions(id,request.permissionIds()==null?Set.of():request.permissionIds(),CurrentActor.userId());RoleResponse result=repository.findRole(id).orElseThrow();activityLogs.appendMutation(CurrentActor.userId(),"CUSTOM_ROLE_CREATE","identity","role",id,"null",json(result));return result;}
    @PreAuthorize("hasAuthority('role.manage')") @Transactional public RoleResponse updateRole(UUID id,UpdateRoleRequest request){requireOwner();RoleResponse before=repository.findRole(id).orElseThrow(()->notFound("ROLE_NOT_FOUND","Role not found"));if(before.systemRole())throw invalid("SYSTEM_ROLE_IMMUTABLE","System role metadata cannot be changed");repository.updateRole(id,request.name().strip(),request.description());RoleResponse after=repository.findRole(id).orElseThrow();activityLogs.appendMutation(CurrentActor.userId(),"CUSTOM_ROLE_UPDATE","identity","role",id,json(before),json(after));return after;}
    @PreAuthorize("hasAuthority('role.manage')") @Transactional public void deactivateRole(UUID id){requireOwner();RoleResponse before=repository.findRole(id).orElseThrow(()->notFound("ROLE_NOT_FOUND","Role not found"));if(before.systemRole())throw invalid("SYSTEM_ROLE_IMMUTABLE","System roles cannot be deactivated");repository.deactivateRole(id);repository.revokeSessionsForRole(id);activityLogs.appendMutation(CurrentActor.userId(),"CUSTOM_ROLE_DEACTIVATE","identity","role",id,json(before),"{\"status\":\"INACTIVE\"}");}

    private UserResponse changeStatus(UUID id,String status,String action){UserResponse before=user(id);UUID actor=CurrentActor.userId();protectOwnerTarget(actor,before);if("LOCKED".equals(status)&&before.roles().contains("OWNER")&&repository.countActiveOwners()<=1)throw new BusinessException(HttpStatus.CONFLICT,"LAST_OWNER_REQUIRED","The last OWNER cannot be locked");repository.setUserStatus(id,status);repository.revokeUserSessions(id,action);UserResponse after=user(id);activityLogs.appendMutation(actor,action,"identity","user",id,json(before),json(after));return after;}
    private void protectOwnerTarget(UUID actor,UserResponse target){if(target.roles().contains("OWNER")&&!repository.userHasRole(actor,"OWNER"))throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_PROTECTED","IT administrators cannot modify OWNER accounts");}
    private void requireOwner(){if(!repository.userHasRole(CurrentActor.userId(),"OWNER"))throw new BusinessException(HttpStatus.FORBIDDEN,"OWNER_REQUIRED","Only OWNER may manage custom roles");}
    private boolean roleIdsContainPrivileged(Set<UUID> ids){return repository.roleIdsContainCode(ids,"OWNER")||repository.roleIdsContainCode(ids,"IT_ADMIN");}
    private String itAdminAction(Set<String>a,Set<String>b){if(!a.contains("IT_ADMIN")&&b.contains("IT_ADMIN"))return "IT_ADMIN_ASSIGN";if(a.contains("IT_ADMIN")&&!b.contains("IT_ADMIN"))return "IT_ADMIN_REVOKE";return "USER_ROLE_REPLACE";}
    private String requiredReason(String reason){if(reason==null||reason.isBlank())throw invalid("REASON_REQUIRED","A reason is required for direct permissions");return reason;}
    private BusinessException invalid(String code,String message){return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,code,message);}

    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize audit snapshot", e); } }
    private BusinessException notFound(String code, String message) { return new BusinessException(HttpStatus.NOT_FOUND, code, message); }
}

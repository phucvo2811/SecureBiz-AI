package com.securebiz.identity.api;

import com.securebiz.identity.service.IdentityAdminService;
import com.securebiz.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class IdentityAdminController {
    private final IdentityAdminService service;
    public IdentityAdminController(IdentityAdminService service) { this.service = service; }

    @GetMapping("/users")
    PageResponse<UserResponse> users(@RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return service.users(search, page, size);
    }
    @PostMapping("/users")
    ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = service.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }
    @GetMapping("/users/{id}") UserResponse user(@PathVariable UUID id){return service.user(id);}
    @PutMapping("/users/{id}") UserResponse updateUser(@PathVariable UUID id,@Valid @RequestBody UpdateUserRequest request){return service.updateUser(id,request);}
    @PutMapping("/users/{id}/roles")
    UserResponse replaceUserRoles(@PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return service.replaceUserRoles(id, request.roleIds());
    }
    @GetMapping("/users/{id}/permissions") UserResponse permissions(@PathVariable UUID id){return service.effectivePermissions(id);}
    @PutMapping("/users/{id}/permissions") UserResponse replacePermissions(@PathVariable UUID id,@Valid @RequestBody UpdateUserPermissionsRequest request){return service.replaceUserPermissions(id,request);}
    @PostMapping("/users/{id}/sessions/revoke") ResponseEntity<Void> revokeSessions(@PathVariable UUID id){service.revokeSessions(id);return ResponseEntity.noContent().build();}
    @PostMapping("/users/{id}/lock") UserResponse lock(@PathVariable UUID id){return service.lock(id);}
    @PostMapping("/users/{id}/unlock") UserResponse unlock(@PathVariable UUID id){return service.unlock(id);}
    @GetMapping("/roles")
    List<RoleResponse> roles() { return service.roles(); }
    @PostMapping("/roles") ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request){RoleResponse result=service.createRole(request);return ResponseEntity.created(URI.create("/api/v1/roles/"+result.id())).body(result);}
    @PutMapping("/roles/{id}") RoleResponse updateRole(@PathVariable UUID id,@Valid @RequestBody UpdateRoleRequest request){return service.updateRole(id,request);}
    @PostMapping("/roles/{id}/deactivate") ResponseEntity<Void> deactivateRole(@PathVariable UUID id){service.deactivateRole(id);return ResponseEntity.noContent().build();}
    @GetMapping("/permissions")
    List<PermissionResponse> permissions() { return service.permissions(); }
    @PutMapping("/roles/{id}/permissions")
    RoleResponse replaceRolePermissions(@PathVariable UUID id, @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return service.replaceRolePermissions(id, request.permissionIds());
    }
}

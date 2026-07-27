package com.manacommunity.auth.controller;

import static com.manacommunity.common.constants.PermissionConstants.*;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.auth.user.service.LoggedInUserService;
import com.manacommunity.auth.service.RolePermissionService;
import com.manacommunity.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final LoggedInUserService loggedInUserService;
    private final RoleService roleService;

    /**
     * GET /api/roles/permissions
     * Returns all roles mapped to their active permission keys, scoped to the caller's community.
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, List<String>>> getAllRolePermissions(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(rolePermissionService.getAllRolePermissions(communityId));
    }

    /**
     * PUT /api/roles/{role}/permissions
     * Overwrites permissions for a role (no userId) or for a specific user override (userId provided).
     */
    @PutMapping("/{role}/permissions")
    public ResponseEntity<Void> updateRolePermissions(
            @PathVariable String role,
            @RequestBody List<String> permissions,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (userId != null) {
            rolePermissionService.updateUserPermissions(userId, role, permissions);
        } else {
            Long communityId = resolveCommunityId(principal);
            rolePermissionService.updateRolePermissions(role, communityId, permissions);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/roles
     * Returns a list of all roles.
     */
    @GetMapping
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * POST /api/roles
     * Creates a new role.
     */
    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> body) {
        String roleName = body.get("name");
        Role saved = roleService.createRole(roleName);
        return ResponseEntity.ok(saved);
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        if (principal == null) return null;
        try {
            AppUser user = loggedInUserService.resolve(principal);
            if (ROLE_SUPER_ADMIN.equalsIgnoreCase(user.getRole())) return null;
            return user.getCommunity() != null ? user.getCommunity().getId() : null;
        } catch (Exception ex) {
            log.warn("Failed to resolve community for principal: {}", ex.getMessage());
            return null;
        }
    }
}

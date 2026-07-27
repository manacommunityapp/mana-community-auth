package com.manacommunity.auth.service;

import com.manacommunity.auth.user.service.LoggedInUserService;

import static com.manacommunity.common.constants.PermissionConstants.*;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.model.RolePermission;
import com.manacommunity.common.repository.RolePermissionRepository;
import com.manacommunity.common.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PermissionCheckService — Centralized programmatic permission checking.
 * Queries the database on every call to verify the user's permissions.
 * SUPER_ADMIN bypasses all checks.
 */
@Service
@RequiredArgsConstructor
public class PermissionCheckService {

    private final RolePermissionRepository rolePermissionRepository;
    private final LoggedInUserService loggedInUserService;

    /**
     * Check if the user has ANY of the required permissions.
     * SUPER_ADMIN always returns true.
     */
    public boolean hasAnyPermission(UserPrincipal principal, String... requiredPermissions) {
        AppUser user = loggedInUserService.resolve(principal);
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(user.getRole())) {
            return true;
        }
        Set<String> userPerms = loadPermissionsFromDB(user);
        return Arrays.stream(requiredPermissions).anyMatch(userPerms::contains);
    }

    /**
     * Same as hasAnyPermission but throws AccessDeniedException if check fails.
     */
    public void requireAnyPermission(UserPrincipal principal, String... requiredPermissions) {
        if (!hasAnyPermission(principal, requiredPermissions)) {
            throw new AccessDeniedException("Insufficient permissions. Required any of: "
                    + String.join(", ", requiredPermissions));
        }
    }

    /**
     * Check if the user has ALL of the required permissions.
     */
    public boolean hasAllPermissions(UserPrincipal principal, String... requiredPermissions) {
        AppUser user = loggedInUserService.resolve(principal);
        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(user.getRole())) {
            return true;
        }
        Set<String> userPerms = loadPermissionsFromDB(user);
        return Arrays.stream(requiredPermissions).allMatch(userPerms::contains);
    }

    /**
     * Load the user's effective permissions from the database.
     * Priority: user-specific permissions > role-based permissions.
     */
    private Set<String> loadPermissionsFromDB(AppUser user) {
        // 1. Check user-specific permissions first
        List<RolePermission> userPerms = rolePermissionRepository.findByUserId(user.getId());
        if (!userPerms.isEmpty()) {
            return userPerms.stream()
                    .map(RolePermission::getPermissionKey)
                    .collect(Collectors.toSet());
        }
        // 2. Fall back to role-based permissions
        List<RolePermission> rolePerms = rolePermissionRepository.findByRoleIgnoreCase(user.getRole());
        return rolePerms.stream()
                .map(RolePermission::getPermissionKey)
                .collect(Collectors.toSet());
    }
}

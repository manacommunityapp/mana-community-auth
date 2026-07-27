package com.manacommunity.auth.service;

import java.util.List;
import java.util.Map;

public interface RolePermissionService {

    /** Returns all role → permission keys, filtered to the given communityId (null = global). */
    Map<String, List<String>> getAllRolePermissions(Long communityId);

    /**
     * Overwrites the role-level permissions for a community-scoped role.
     * Pass communityId = null for global (SUPER_ADMIN) roles.
     */
    void updateRolePermissions(String roleName, Long communityId, List<String> permissions);

    /** Overwrites user-specific permission overrides for the given user. */
    void updateUserPermissions(Long userId, String role, List<String> permissions);

    /** Returns the user-specific permission keys for a given user. */
    List<String> getUserPermissions(Long userId);
}

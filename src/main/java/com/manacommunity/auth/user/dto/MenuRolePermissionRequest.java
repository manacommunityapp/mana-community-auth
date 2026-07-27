package com.manacommunity.auth.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO — set or update CRUD flags for a single role + menu item.
 */
public record MenuRolePermissionRequest(
    @NotNull Long roleId,
    @NotNull Long menuId,
    Boolean canView,
    Boolean canAdd,
    Boolean canUpdate,
    Boolean canDelete
) {}

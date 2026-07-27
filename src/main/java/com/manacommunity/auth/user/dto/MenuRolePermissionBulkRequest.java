package com.manacommunity.auth.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Bulk-assign request — set permissions for one role across multiple menu items at once.
 * Used by the admin "permission matrix" UI grid.
 */
public record MenuRolePermissionBulkRequest(
    @NotNull Long roleId,
    @NotNull List<MenuPermissionEntry> permissions
) {
    public record MenuPermissionEntry(
        @NotNull Long menuId,
        Boolean canView,
        Boolean canAdd,
        Boolean canUpdate,
        Boolean canDelete
    ) {}
}

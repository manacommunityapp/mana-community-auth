package com.manacommunity.auth.user.controller;

import com.manacommunity.auth.user.dto.MenuRolePermissionBulkRequest;
import com.manacommunity.auth.user.dto.MenuRolePermissionRequest;
import com.manacommunity.auth.user.dto.MenuRolePermissionResponse;
import com.manacommunity.auth.user.service.MenuRolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <pre>
 * GET    /api/menu-permissions                          — full matrix (all roles × all menus)
 * GET    /api/menu-permissions/role/{roleId}             — matrix for one role
 * GET    /api/menu-permissions/menu/{menuId}             — matrix for one menu item
 * GET    /api/menu-permissions/role/{roleId}/menu/{menuId} — single cell
 * GET    /api/menu-permissions/role/{roleId}/viewable    — sidebar: viewable menus for role
 * POST   /api/menu-permissions                          — upsert single cell
 * POST   /api/menu-permissions/bulk                     — upsert entire row (one role, many menus)
 * DELETE /api/menu-permissions/{id}                     — delete by id
 * DELETE /api/menu-permissions/role/{roleId}/menu/{menuId} — delete by role+menu
 * DELETE /api/menu-permissions/role/{roleId}             — delete all for a role
 * </pre>
 *
 * All write operations are SUPER_ADMIN-only. Read operations are available to
 * any authenticated user (the frontend needs them to build the sidebar and
 * enable/disable action buttons).
 */
@RestController
@RequestMapping("/api/menu-permissions")
@RequiredArgsConstructor
public class MenuRolePermissionController {

    private final MenuRolePermissionService service;

    // ─── Read (any authenticated user) ───────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<MenuRolePermissionResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<MenuRolePermissionResponse>> getByRole(@PathVariable Long roleId) {
        return ResponseEntity.ok(service.getByRole(roleId));
    }

    @GetMapping("/menu/{menuId}")
    public ResponseEntity<List<MenuRolePermissionResponse>> getByMenu(@PathVariable Long menuId) {
        return ResponseEntity.ok(service.getByMenu(menuId));
    }

    @GetMapping("/role/{roleId}/menu/{menuId}")
    public ResponseEntity<MenuRolePermissionResponse> getByRoleAndMenu(
            @PathVariable Long roleId, @PathVariable Long menuId) {
        return ResponseEntity.ok(service.getByRoleAndMenu(roleId, menuId));
    }

    /** Returns only the menu items this role can view — used to build the sidebar. */
    @GetMapping("/role/{roleId}/viewable")
    public ResponseEntity<List<MenuRolePermissionResponse>> getViewable(@PathVariable Long roleId) {
        return ResponseEntity.ok(service.getViewableMenus(roleId));
    }

    // ─── Write (SUPER_ADMIN only) ────────────────────────────────────────────

    /** Create or update a single role × menu permission cell. */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MenuRolePermissionResponse> upsert(
            @Valid @RequestBody MenuRolePermissionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upsert(req));
    }

    /** Bulk create/update — one role, many menu items (the "Save" button on the admin matrix grid). */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<MenuRolePermissionResponse>> bulkUpsert(
            @Valid @RequestBody MenuRolePermissionBulkRequest req) {
        return ResponseEntity.ok(service.bulkUpsert(req));
    }

    // ─── Delete (SUPER_ADMIN only) ───────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/role/{roleId}/menu/{menuId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteByRoleAndMenu(
            @PathVariable Long roleId, @PathVariable Long menuId) {
        service.deleteByRoleAndMenu(roleId, menuId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/role/{roleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAllForRole(@PathVariable Long roleId) {
        service.deleteAllForRole(roleId);
        return ResponseEntity.noContent().build();
    }
}

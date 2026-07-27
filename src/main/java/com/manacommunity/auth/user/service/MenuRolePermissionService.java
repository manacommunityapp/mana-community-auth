package com.manacommunity.auth.user.service;

import com.manacommunity.auth.user.dto.MenuRolePermissionBulkRequest;
import com.manacommunity.auth.user.dto.MenuRolePermissionRequest;
import com.manacommunity.auth.user.dto.MenuRolePermissionResponse;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.auth.user.model.MenuItem;
import com.manacommunity.auth.user.model.MenuRolePermission;
import com.manacommunity.common.model.Role;
import com.manacommunity.auth.user.repository.MenuItemRepository;
import com.manacommunity.auth.user.repository.MenuRolePermissionRepository;
import com.manacommunity.auth.repository.RoleRepository;
import com.manacommunity.common.enums.AuditAction;
import com.manacommunity.common.enums.AuditModule;
import com.manacommunity.common.security.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuRolePermissionService {

    private final MenuRolePermissionRepository permRepo;
    private final RoleRepository roleRepo;
    private final MenuItemRepository menuItemRepo;
    private final AuditService auditService;

    // ─── Read ────────────────────────────────────────────────────────────────

    /** Full permission matrix for one role (all menu items it has any flags on). */
    @Transactional(readOnly = true)
    public List<MenuRolePermissionResponse> getByRole(Long roleId) {
        return permRepo.findByRoleId(roleId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Full permission matrix for one menu item (all roles that have flags on it). */
    @Transactional(readOnly = true)
    public List<MenuRolePermissionResponse> getByMenu(Long menuId) {
        return permRepo.findByMenuItemId(menuId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Single permission row. */
    @Transactional(readOnly = true)
    public MenuRolePermissionResponse getByRoleAndMenu(Long roleId, Long menuId) {
        MenuRolePermission perm = permRepo.findByRoleIdAndMenuItemId(roleId, menuId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MenuRolePermission", "roleId", roleId + "-menuId=" + menuId));
        return toResponse(perm);
    }

    /** All viewable menu items for a role (used to build the sidebar). */
    @Transactional(readOnly = true)
    public List<MenuRolePermissionResponse> getViewableMenus(Long roleId) {
        return permRepo.findViewableByRoleId(roleId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Complete matrix — all roles × all menu items. */
    @Transactional(readOnly = true)
    public List<MenuRolePermissionResponse> getAll() {
        return permRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Permission check helpers (for use in controllers/services) ──────────

    @Transactional(readOnly = true)
    public boolean canView(Long roleId, Long menuId)   { return permRepo.canView(roleId, menuId); }
    @Transactional(readOnly = true)
    public boolean canAdd(Long roleId, Long menuId)    { return permRepo.canAdd(roleId, menuId); }
    @Transactional(readOnly = true)
    public boolean canUpdate(Long roleId, Long menuId) { return permRepo.canUpdate(roleId, menuId); }
    @Transactional(readOnly = true)
    public boolean canDelete(Long roleId, Long menuId) { return permRepo.canDelete(roleId, menuId); }

    // ─── Create / Update (single) ────────────────────────────────────────────

    @Transactional
    public MenuRolePermissionResponse upsert(MenuRolePermissionRequest req) {
        Role role = roleRepo.findById(req.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", req.roleId()));
        MenuItem menuItem = menuItemRepo.findById(req.menuId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", req.menuId()));

        MenuRolePermission perm = permRepo.findByRoleIdAndMenuItemId(req.roleId(), req.menuId())
                .orElse(MenuRolePermission.builder()
                        .role(role)
                        .menuItem(menuItem)
                        .build());

        String oldValue = perm.getId() != null
                ? "view=" + perm.getCanView() + ", add=" + perm.getCanAdd()
                  + ", update=" + perm.getCanUpdate() + ", delete=" + perm.getCanDelete()
                : null;

        perm.setCanView(req.canView() != null ? req.canView() : false);
        perm.setCanAdd(req.canAdd() != null ? req.canAdd() : false);
        perm.setCanUpdate(req.canUpdate() != null ? req.canUpdate() : false);
        perm.setCanDelete(req.canDelete() != null ? req.canDelete() : false);

        MenuRolePermission saved = permRepo.save(perm);

        String newValue = "view=" + saved.getCanView() + ", add=" + saved.getCanAdd()
                + ", update=" + saved.getCanUpdate() + ", delete=" + saved.getCanDelete();

        auditService.record(AuditAction.PERMISSION_CHANGED, AuditModule.ADMIN,
                "MenuRolePermission", String.valueOf(saved.getId()),
                oldValue, "role=" + role.getName() + ", menu=" + menuItem.getMenuKey() + ", " + newValue);

        return toResponse(saved);
    }

    // ─── Bulk update (one role, many menu items — the "matrix save" endpoint) ─

    @Transactional
    public List<MenuRolePermissionResponse> bulkUpsert(MenuRolePermissionBulkRequest req) {
        Role role = roleRepo.findById(req.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", req.roleId()));

        List<MenuRolePermissionResponse> results = req.permissions().stream().map(entry -> {
            MenuItem menuItem = menuItemRepo.findById(entry.menuId())
                    .orElseThrow(() -> new ResourceNotFoundException("MenuItem", entry.menuId()));

            MenuRolePermission perm = permRepo.findByRoleIdAndMenuItemId(req.roleId(), entry.menuId())
                    .orElse(MenuRolePermission.builder()
                            .role(role)
                            .menuItem(menuItem)
                            .build());

            perm.setCanView(entry.canView() != null ? entry.canView() : false);
            perm.setCanAdd(entry.canAdd() != null ? entry.canAdd() : false);
            perm.setCanUpdate(entry.canUpdate() != null ? entry.canUpdate() : false);
            perm.setCanDelete(entry.canDelete() != null ? entry.canDelete() : false);

            return toResponse(permRepo.save(perm));
        }).toList();

        auditService.record(AuditAction.PERMISSION_CHANGED, AuditModule.ADMIN,
                "MenuRolePermission", "bulk",
                null, "role=" + role.getName() + ", menuItems=" + results.size());

        log.info("Bulk-upserted {} menu permissions for role {}", results.size(), role.getName());
        return results;
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        MenuRolePermission perm = permRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuRolePermission", id));

        String snapshot = "role=" + perm.getRole().getName()
                + ", menu=" + perm.getMenuItem().getMenuKey()
                + ", view=" + perm.getCanView() + ", add=" + perm.getCanAdd()
                + ", update=" + perm.getCanUpdate() + ", delete=" + perm.getCanDelete();

        permRepo.delete(perm);

        auditService.record(AuditAction.PERMISSION_CHANGED, AuditModule.ADMIN,
                "MenuRolePermission", String.valueOf(id), snapshot, null);
    }

    @Transactional
    public void deleteByRoleAndMenu(Long roleId, Long menuId) {
        permRepo.deleteByRoleIdAndMenuItemId(roleId, menuId);
    }

    @Transactional
    public void deleteAllForRole(Long roleId) {
        permRepo.deleteByRoleId(roleId);
        auditService.record(AuditAction.PERMISSION_CHANGED, AuditModule.ADMIN,
                "MenuRolePermission", "role=" + roleId, "all permissions", null);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    private MenuRolePermissionResponse toResponse(MenuRolePermission p) {
        return new MenuRolePermissionResponse(
                p.getId(),
                p.getRole().getId(),
                p.getRole().getName(),
                p.getMenuItem().getId(),
                p.getMenuItem().getMenuKey(),
                p.getMenuItem().getLabel(),
                p.getCanView(),
                p.getCanAdd(),
                p.getCanUpdate(),
                p.getCanDelete()
        );
    }
}

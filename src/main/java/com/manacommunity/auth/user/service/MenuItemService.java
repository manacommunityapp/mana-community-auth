package com.manacommunity.auth.user.service;

import com.manacommunity.common.model.Role;

import com.manacommunity.auth.user.dto.MenuItemRequest;
import com.manacommunity.auth.user.dto.MenuItemResponse;
import com.manacommunity.auth.user.dto.MenuRolePermissionResponse;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.model.Community;
import com.manacommunity.auth.user.model.MenuItem;
import com.manacommunity.auth.user.model.MenuRolePermission;
import com.manacommunity.auth.repository.CommunityRepository;
import com.manacommunity.auth.user.repository.MenuItemRepository;
import com.manacommunity.auth.user.repository.MenuRolePermissionRepository;
import com.manacommunity.common.enums.AuditAction;
import com.manacommunity.common.enums.AuditModule;
import com.manacommunity.common.security.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepo;
    private final MenuRolePermissionRepository menuRolePermRepo;
    private final CommunityRepository communityRepo;
    private final AuditService auditService;

    // ─── Read ────────────────────────────────────────────────────────────────

    /**
     * Returns the full menu tree filtered by the caller's role.
     * Visibility is determined by the menu_role_permission table (can_view = true),
     * NOT the old string-based permission_key field.
     *
     * @param roleId      the caller's role id (from Role entity)
     * @param communityId the caller's community (null for super-admin / global)
     */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuTree(Long roleId, Long communityId) {
        // Get the set of menu item IDs this role can view
        Set<Long> viewableMenuIds = menuRolePermRepo.findViewableByRoleId(roleId).stream()
                .map(p -> p.getMenuItem().getId())
                .collect(Collectors.toSet());

        List<MenuItem> roots = communityId != null
                ? menuItemRepo.findActiveRootsByCommunity(communityId)
                : menuItemRepo.findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();

        return roots.stream()
                .filter(item -> viewableMenuIds.contains(item.getId()))
                .map(item -> toResponse(item, viewableMenuIds))
                .toList();
    }

    /**
     * Returns the full menu tree with CRUD flags per item for the given role.
     * Used by the frontend to know which action buttons (Add / Edit / Delete)
     * to show on each menu section.
     */
    @Transactional(readOnly = true)
    public List<MenuItemWithPermissionsResponse> getMenuTreeWithPermissions(Long roleId, Long communityId) {
        List<MenuRolePermission> perms = menuRolePermRepo.findByRoleId(roleId);
        var permMap = perms.stream()
                .collect(Collectors.toMap(p -> p.getMenuItem().getId(), p -> p));

        Set<Long> viewableMenuIds = perms.stream()
                .filter(p -> Boolean.TRUE.equals(p.getCanView()))
                .map(p -> p.getMenuItem().getId())
                .collect(Collectors.toSet());

        List<MenuItem> roots = communityId != null
                ? menuItemRepo.findActiveRootsByCommunity(communityId)
                : menuItemRepo.findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();

        return roots.stream()
                .filter(item -> viewableMenuIds.contains(item.getId()))
                .map(item -> toResponseWithPerms(item, viewableMenuIds, permMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getById(Long id) {
        MenuItem item = menuItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
        return toResponse(item, null);
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getByMenuKey(String menuKey) {
        MenuItem item = menuItemRepo.findByMenuKey(menuKey)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "menuKey", menuKey));
        return toResponse(item, null);
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Transactional
    public MenuItemResponse create(MenuItemRequest req) {
        if (menuItemRepo.existsByMenuKey(req.menuKey())) {
            throw new IllegalArgumentException("Menu key '" + req.menuKey() + "' already exists.");
        }

        MenuItem item = MenuItem.builder()
                .menuKey(req.menuKey())
                .label(req.label())
                .icon(req.icon())
                .routePath(req.routePath())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .isActive(req.isActive() != null ? req.isActive() : true)
                .permissionKey(req.permissionKey())
                .build();

        if (req.parentId() != null) {
            MenuItem parent = menuItemRepo.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent MenuItem", req.parentId()));
            item.setParent(parent);
        }

        if (req.communityId() != null) {
            Community community = communityRepo.findById(req.communityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.communityId()));
            item.setCommunity(community);
        }

        MenuItem saved = menuItemRepo.save(item);
        log.info("Created menu item: key={}, label={}, parentId={}", saved.getMenuKey(), saved.getLabel(), req.parentId());

        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "MenuItem", String.valueOf(saved.getId()), null,
                "key=" + saved.getMenuKey() + ", label=" + saved.getLabel());

        return toResponse(saved, null);
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest req) {
        MenuItem item = menuItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));

        String oldValue = "key=" + item.getMenuKey() + ", label=" + item.getLabel()
                + ", sortOrder=" + item.getSortOrder() + ", active=" + item.getIsActive();

        item.setMenuKey(req.menuKey());
        item.setLabel(req.label());
        item.setIcon(req.icon());
        item.setRoutePath(req.routePath());
        if (req.sortOrder() != null) item.setSortOrder(req.sortOrder());
        if (req.isActive() != null) item.setIsActive(req.isActive());
        item.setPermissionKey(req.permissionKey());

        if (req.parentId() != null) {
            if (req.parentId().equals(id)) {
                throw new IllegalArgumentException("A menu item cannot be its own parent.");
            }
            MenuItem parent = menuItemRepo.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent MenuItem", req.parentId()));
            item.setParent(parent);
        } else {
            item.setParent(null);
        }

        if (req.communityId() != null) {
            Community community = communityRepo.findById(req.communityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.communityId()));
            item.setCommunity(community);
        } else {
            item.setCommunity(null);
        }

        MenuItem saved = menuItemRepo.save(item);
        String newValue = "key=" + saved.getMenuKey() + ", label=" + saved.getLabel()
                + ", sortOrder=" + saved.getSortOrder() + ", active=" + saved.getIsActive();

        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "MenuItem", String.valueOf(id), oldValue, newValue);

        return toResponse(saved, null);
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        MenuItem item = menuItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));

        String snapshot = "key=" + item.getMenuKey() + ", label=" + item.getLabel();
        menuItemRepo.delete(item);

        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.ADMIN,
                "MenuItem", String.valueOf(id), snapshot, null);
    }

    // ─── Reorder ─────────────────────────────────────────────────────────────

    @Transactional
    public void reorder(List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            int order = i;
            menuItemRepo.findById(orderedIds.get(i)).ifPresent(item -> {
                item.setSortOrder(order);
                menuItemRepo.save(item);
            });
        }
    }

    // ─── Mapping helpers ─────────────────────────────────────────────────────

    private MenuItemResponse toResponse(MenuItem item, Set<Long> viewableMenuIds) {
        List<MenuItemResponse> childDtos = item.getChildren() == null
                ? List.of()
                : item.getChildren().stream()
                    .filter(MenuItem::getIsActive)
                    .filter(child -> viewableMenuIds == null || viewableMenuIds.contains(child.getId()))
                    .map(child -> toResponse(child, viewableMenuIds))
                    .toList();

        return new MenuItemResponse(
                item.getId(), item.getMenuKey(), item.getLabel(),
                item.getIcon(), item.getRoutePath(),
                item.getParent() != null ? item.getParent().getId() : null,
                item.getSortOrder(), item.getIsActive(), item.getPermissionKey(),
                childDtos
        );
    }

    private MenuItemWithPermissionsResponse toResponseWithPerms(
            MenuItem item, Set<Long> viewableMenuIds,
            java.util.Map<Long, MenuRolePermission> permMap) {

        MenuRolePermission perm = permMap.get(item.getId());

        List<MenuItemWithPermissionsResponse> childDtos = item.getChildren() == null
                ? List.of()
                : item.getChildren().stream()
                    .filter(MenuItem::getIsActive)
                    .filter(child -> viewableMenuIds.contains(child.getId()))
                    .map(child -> toResponseWithPerms(child, viewableMenuIds, permMap))
                    .toList();

        return new MenuItemWithPermissionsResponse(
                item.getId(), item.getMenuKey(), item.getLabel(),
                item.getIcon(), item.getRoutePath(),
                item.getParent() != null ? item.getParent().getId() : null,
                item.getSortOrder(),
                perm != null && Boolean.TRUE.equals(perm.getCanView()),
                perm != null && Boolean.TRUE.equals(perm.getCanAdd()),
                perm != null && Boolean.TRUE.equals(perm.getCanUpdate()),
                perm != null && Boolean.TRUE.equals(perm.getCanDelete()),
                childDtos
        );
    }

    /** Menu tree node with inline CRUD flags — what the frontend actually needs. */
    public record MenuItemWithPermissionsResponse(
        Long id, String menuKey, String label, String icon, String routePath,
        Long parentId, Integer sortOrder,
        boolean canView, boolean canAdd, boolean canUpdate, boolean canDelete,
        List<MenuItemWithPermissionsResponse> children
    ) {}
}

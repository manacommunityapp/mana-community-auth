package com.manacommunity.auth.service.impl;

import com.manacommunity.common.enums.AuditAction;

import com.manacommunity.common.enums.AuditModule;

import com.manacommunity.common.security.AuditService;

import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.model.RolePermission;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.common.repository.RolePermissionRepository;
import com.manacommunity.auth.repository.RoleRepository;
import com.manacommunity.auth.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final AppUserRepository appUserRepo;
    private final com.manacommunity.common.security.AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getAllRolePermissions(Long communityId) {
        return rolePermissionRepo.findAll().stream()
                .filter(rp -> rp.getUser() == null)
                .filter(rp -> {
                    Role roleEntity = rp.getRoleEntity();
                    if (roleEntity == null) return true;
                    if (communityId != null) {
                        return roleEntity.getCommunityId() == null
                                || roleEntity.getCommunityId().equals(communityId);
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(
                        rp -> rp.getRole().toUpperCase(),
                        Collectors.mapping(RolePermission::getPermissionKey, Collectors.toList())
                ));
    }

    @Override
    @Transactional
    public void updateRolePermissions(String roleName, Long communityId, List<String> permissions) {
        Role role = communityId != null
                ? roleRepo.findByNameIgnoreCaseAndCommunityId(roleName, communityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName))
                : roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        rolePermissionRepo.deleteByRoleEntityIdAndUserIsNull(role.getId());

        List<RolePermission> entities = permissions.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .distinct()
                .map(p -> RolePermission.builder()
                        .role(roleName.toUpperCase())
                        .roleEntity(role)
                        .permissionKey(p)
                        .build())
                .toList();

        rolePermissionRepo.saveAll(entities);
        auditService.record(
                com.manacommunity.common.enums.AuditAction.PERMISSION_CHANGED,
                com.manacommunity.common.enums.AuditModule.ADMIN,
                "Role", roleName,
                null,
                "permissions=" + entities.size() + " (role-level, community=" + communityId + ")");
    }

    @Override
    @Transactional
    public void updateUserPermissions(Long userId, String role, List<String> permissions) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        rolePermissionRepo.deleteByUserId(userId);

        List<RolePermission> entities = permissions.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .distinct()
                .map(p -> RolePermission.builder()
                        .role(role.toUpperCase())
                        .roleEntity(user.getRoleEntity())
                        .permissionKey(p)
                        .user(user)
                        .build())
                .toList();

        rolePermissionRepo.saveAll(entities);
        auditService.record(
                com.manacommunity.common.enums.AuditAction.PERMISSION_CHANGED,
                com.manacommunity.common.enums.AuditModule.ADMIN,
                "AppUser", String.valueOf(userId),
                null,
                "permissions=" + entities.size() + " (user-level override, role=" + role + ")");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserPermissions(Long userId) {
        return rolePermissionRepo.findByUserId(userId).stream()
                .map(RolePermission::getPermissionKey)
                .toList();
    }
}

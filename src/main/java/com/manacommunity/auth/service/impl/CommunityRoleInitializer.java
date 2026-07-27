package com.manacommunity.auth.service.impl;

import com.manacommunity.common.constants.PermissionConstants;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.model.RolePermission;
import com.manacommunity.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityRoleInitializer {

    private final RoleRepository roleRepo;

    private static final Map<String, List<String>> ROLE_PERMISSIONS_MAP = Map.of(
            PermissionConstants.ROLE_ADMIN, PermissionConstants.ADMIN_PERMISSIONS,
            PermissionConstants.ROLE_SPORTS_ADMIN, PermissionConstants.SPORTS_ADMIN_PERMISSIONS,
            PermissionConstants.ROLE_MEMBER, PermissionConstants.MEMBER_PERMISSIONS,
            PermissionConstants.ROLE_VENDOR, PermissionConstants.VENDOR_PERMISSIONS,
            PermissionConstants.ROLE_CASHIER, PermissionConstants.CASHIER_PERMISSIONS,
            PermissionConstants.ROLE_STAFF, PermissionConstants.STAFF_PERMISSIONS
    );

    @Transactional
    public void initializeCommunityRoles(Community community) {
        if (community == null || community.getId() == null) {
            log.warn("Cannot initialize roles: community or community ID is null");
            return;
        }

        log.info("Initializing baseline roles for community: {} (ID: {})", community.getName(), community.getId());
        Long communityId = community.getId();

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS_MAP.entrySet()) {
            String roleName = entry.getKey();
            List<String> perms = entry.getValue();

            // Check if this community role already exists
            boolean exists = roleRepo.existsByNameIgnoreCaseAndCommunityId(roleName, communityId);
            if (!exists) {
                Role role = Role.builder()
                        .name(roleName)
                        .communityId(communityId)
                        .permissions(new HashSet<>())
                        .build();

                for (String p : perms) {
                    RolePermission rp = RolePermission.builder()
                            .role(roleName)
                            .permissionKey(p)
                            .roleEntity(role)
                            .build();
                    role.getPermissions().add(rp);
                }

                roleRepo.save(role);
                log.info("Created community-scoped role: {} for community ID: {}", roleName, communityId);
            }
        }
    }
}

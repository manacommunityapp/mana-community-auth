package com.manacommunity.auth.user.controller;

import com.manacommunity.common.model.Role;

import com.manacommunity.common.user.repository.AppUserRepository;

import static com.manacommunity.common.constants.PermissionConstants.*;
import com.manacommunity.common.dto.PagedResponse;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.auth.user.dto.UserResponse;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.auth.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final LoggedInUserService loggedInUserService;
    private final com.manacommunity.common.repository.RolePermissionRepository rolePermissionRepo;
    private final com.manacommunity.auth.service.RoleService roleService;
    private final com.manacommunity.auth.service.CommunityModuleService communityModuleService;

    private java.util.List<String> getPermissionsForUser(AppUser user) {
        if (user.getRole() == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<com.manacommunity.common.model.RolePermission> userPerms = rolePermissionRepo.findByUserId(user.getId());
        if (!userPerms.isEmpty()) {
            return userPerms.stream()
                    .map(com.manacommunity.common.model.RolePermission::getPermissionKey)
                    .toList();
        }
        if (user.getRoleEntity() != null) {
            return user.getRoleEntity().getPermissions().stream()
                    .map(com.manacommunity.common.model.RolePermission::getPermissionKey)
                    .toList();
        }
        return rolePermissionRepo.findByRoleIgnoreCase(user.getRole()).stream()
                .filter(rp -> rp.getUser() == null)
                .map(com.manacommunity.common.model.RolePermission::getPermissionKey)
                .toList();
    }

    private java.util.List<String> getEnabledModules(AppUser user) {
        if (user.getCommunity() == null) {
            return java.util.Collections.emptyList();
        }
        return communityModuleService.getEnabledModuleKeys(user.getCommunity().getId());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getLoggedInUserDetails(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .kycStatus(user.getKycStatus())
                .profilePicUrl(user.getProfilePicUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .flatNo(user.getFlatNo())
                .block(user.getBlock())
                .communityId(user.getCommunity() != null ? user.getCommunity().getId() : null)
                .roleId(user.getRoleEntity() != null ? user.getRoleEntity().getId() : null)
                .permissions(getPermissionsForUser(user))
                .enabledModules(getEnabledModules(user))
                .build();

        return ResponseEntity.ok(response);
    }

    private final com.manacommunity.common.user.repository.AppUserRepository appUserRepo;
    private final com.manacommunity.auth.user.service.AdminUserService adminUserService;

    @GetMapping("/search")
    public ResponseEntity<java.util.List<UserResponse>> searchUsers(
            @RequestParam(required = false) Long communityId,
            @RequestParam String query,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        final Long finalCommId = targetCommunityId;
        return ResponseEntity.ok(appUserRepo.findByCommunityIdAndFullNameContainingIgnoreCase(finalCommId, query)
                .stream().map(u -> UserResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .role(u.getRole())
                        .communityId(finalCommId)
                        .roleId(u.getRoleEntity() != null ? u.getRoleEntity().getId() : null)
                        .isActive(u.getIsActive())
                        .permissions(getPermissionsForUser(u))
                        .build()).toList());
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<java.util.List<UserResponse>> getCommunityUsers(
            @PathVariable Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (targetCommunityId == null || !targetCommunityId.equals(communityId)) {
                throw new com.manacommunity.common.exception.UnauthorizedActionException(
                        "You can only view users from your own community.");
            }
        }
        final Long finalCommId = targetCommunityId;
        return ResponseEntity.ok(appUserRepo.findByCommunityId(finalCommId)
                .stream().map(u -> UserResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .role(u.getRole())
                        .communityId(finalCommId)
                        .roleId(u.getRoleEntity() != null ? u.getRoleEntity().getId() : null)
                        .isActive(u.getIsActive())
                        .permissions(getPermissionsForUser(u))
                        .build()).toList());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String kycStatus,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(loggedInUser.getRole());
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("fullName").ascending());

        Page<AppUser> userPage;
        if (isSuperAdmin) {
            if (kycStatus != null && !kycStatus.trim().isEmpty()) {
                userPage = appUserRepo.findByKycStatus(kycStatus.toUpperCase(), pageable);
            } else {
                userPage = appUserRepo.findAll(pageable);
            }
        } else {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(PagedResponse.empty());
            }
            if (kycStatus != null && !kycStatus.trim().isEmpty()) {
                userPage = appUserRepo.findByCommunityIdAndKycStatus(communityId, kycStatus.toUpperCase(), pageable);
            } else {
                userPage = appUserRepo.findByCommunityId(communityId, pageable);
            }
        }
        return ResponseEntity.ok(PagedResponse.from(userPage, u -> UserResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .phone(u.getPhone())
                        .role(u.getRole())
                        .kycStatus(u.getKycStatus())
                        .profilePicUrl(u.getProfilePicUrl())
                        .gender(u.getGender())
                        .dateOfBirth(u.getDateOfBirth())
                        .flatNo(u.getFlatNo())
                        .block(u.getBlock())
                        .communityId(u.getCommunity() != null ? u.getCommunity().getId() : null)
                        .roleId(u.getRoleEntity() != null ? u.getRoleEntity().getId() : null)
                        .isActive(u.getIsActive())
                        .permissions(getPermissionsForUser(u))
                        .build()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @jakarta.validation.Valid @RequestBody com.manacommunity.auth.user.dto.AdminCreateUserRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        // Non-super-admins may only create users within their own community.
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole()) && loggedInUser.getCommunity() != null) {
            req.setCommunityId(loggedInUser.getCommunity().getId());
            req.setInviteCode(null);
        }

        AppUser saved = adminUserService.createUser(req);
        UserResponse body = UserResponse.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .phone(saved.getPhone())
                .role(saved.getRole())
                .kycStatus(saved.getKycStatus())
                .profilePicUrl(saved.getProfilePicUrl())
                .gender(saved.getGender())
                .dateOfBirth(saved.getDateOfBirth())
                .flatNo(saved.getFlatNo())
                .block(saved.getBlock())
                .communityId(saved.getCommunity() != null ? saved.getCommunity().getId() : null)
                .roleId(saved.getRoleEntity() != null ? saved.getRoleEntity().getId() : null)
                .isActive(saved.getIsActive())
                .permissions(getPermissionsForUser(saved))
                .build();
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("User", id));
        return ResponseEntity.ok(UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .kycStatus(user.getKycStatus())
                .profilePicUrl(user.getProfilePicUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .flatNo(user.getFlatNo())
                .block(user.getBlock())
                .communityId(user.getCommunity() != null ? user.getCommunity().getId() : null)
                .roleId(user.getRoleEntity() != null ? user.getRoleEntity().getId() : null)
                .isActive(user.getIsActive())
                .permissions(getPermissionsForUser(user))
                .build());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("User", id));
        user.setIsActive(!user.getIsActive());
        appUserRepo.save(user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> updateUserRole(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("User", id));
        String newRole = body.get("role");
        if (newRole == null) {
            throw new com.manacommunity.common.exception.InvalidInputException("Role is required");
        }
        
        String normRole = newRole.toUpperCase();
        user.setRole(normRole);
        
        // Resolve and update roleEntity scoped to user's community
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        com.manacommunity.common.model.Role roleEntity = roleService.findOrCreateRole(normRole, communityId);
        if (roleEntity == null) {
            throw new com.manacommunity.common.exception.InvalidInputException("Failed to resolve role: " + normRole);
        }
        user.setRoleEntity(roleEntity);
        appUserRepo.save(user);

        // Delete old user-specific permissions
        rolePermissionRepo.deleteByUserId(user.getId());
        rolePermissionRepo.flush();

        // Load standard role permission templates (where user is null) from the resolved roleEntity
        java.util.Set<com.manacommunity.common.model.RolePermission> templates =
                roleEntity.getPermissions() != null ? roleEntity.getPermissions() : java.util.Collections.emptySet();
        
        // Create and save user-specific role permissions
        java.util.List<com.manacommunity.common.model.RolePermission> userPermissions = templates.stream()
                .filter(t -> t.getUser() == null) // copy from generic template
                .map(t -> t.getPermissionKey())
                .filter(pk -> pk != null && !pk.trim().isEmpty())
                .distinct()
                .map(pk -> com.manacommunity.common.model.RolePermission.builder()
                        .role(normRole)
                        .roleEntity(roleEntity)
                        .permissionKey(pk)
                        .user(user)
                        .build())
                .toList();
        
        rolePermissionRepo.saveAll(userPermissions);
        
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/kyc")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<Void> updateUserKycStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("User", id));
        String status = body.get("status");
        if (status == null || (!status.equals("VERIFIED") && !status.equals("REJECTED") && !status.equals("PENDING"))) {
            throw new com.manacommunity.common.exception.InvalidInputException("Valid status (PENDING, VERIFIED, REJECTED) is required");
        }
        user.setKycStatus(status);
        appUserRepo.save(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/kyc/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<com.manacommunity.auth.user.dto.KycStatsResponse> getKycStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(loggedInUser.getRole());

        long total;
        long pending;
        long approved;
        long rejected;

        if (isSuperAdmin) {
            total = appUserRepo.count();
            approved = appUserRepo.countByKycStatus("VERIFIED");
            rejected = appUserRepo.countByKycStatus("REJECTED");
            pending = Math.max(0, total - approved - rejected);
        } else {
            Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
            if (communityId == null) {
                return ResponseEntity.ok(com.manacommunity.auth.user.dto.KycStatsResponse.builder().build());
            }
            total = appUserRepo.countByCommunityId(communityId);
            approved = appUserRepo.countByCommunityIdAndKycStatus(communityId, "VERIFIED");
            rejected = appUserRepo.countByCommunityIdAndKycStatus(communityId, "REJECTED");
            pending = Math.max(0, total - approved - rejected);
        }

        return ResponseEntity.ok(com.manacommunity.auth.user.dto.KycStatsResponse.builder()
                .total(total)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .build());
    }
}

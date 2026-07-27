package com.manacommunity.auth.user.service.impl;

import static com.manacommunity.common.constants.PermissionConstants.*;

import com.manacommunity.common.exception.DuplicateResourceException;
import com.manacommunity.common.exception.InvalidInputException;
import com.manacommunity.auth.exception.InvalidInviteCodeException;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.model.RolePermission;
import com.manacommunity.auth.repository.CommunityRepository;
import com.manacommunity.common.repository.RolePermissionRepository;
import com.manacommunity.common.security.AuditLogService;
import com.manacommunity.common.validation.PasswordPolicy;
import com.manacommunity.auth.service.RoleService;
import com.manacommunity.auth.user.dto.AdminCreateUserRequest;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.auth.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    /** Used when the admin leaves the password blank; the user resets it later. */
    private static final String DEFAULT_TEMP_PASSWORD = "TempPass123!";

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final RoleService roleService;
    private final RolePermissionRepository rolePermissionRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLog;

    @Override
    @Transactional
    public AppUser createUser(AdminCreateUserRequest req) {
        // 1. Resolve the target community (by id, else invite code).
        Community community = resolveCommunity(req);

        // 2. Duplicate email / phone check (both UNIQUE in the DB).
        String email = req.getEmail().trim();
        String phone = req.getPhone().trim();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("User", "phone", phone);
        }

        String fullName = (req.getFirstName().trim() + " " + req.getLastName().trim()).trim();

        // 3. Password (temporary when blank) + strength policy (also rejects
        //    passwords derived from the user's own data).
        String rawPassword = (req.getPassword() == null || req.getPassword().isBlank())
                ? DEFAULT_TEMP_PASSWORD : req.getPassword();
        PasswordPolicy.validate(rawPassword, Arrays.asList(email, fullName, phone, community.getName()));

        // 4. Resolve the role (UI value -> backend role) scoped to the community.
        String backendRole = mapRole(req.getRole());
        Role roleEntity = roleService.findOrCreateRole(backendRole, community.getId());
        if (roleEntity == null) {
            throw new InvalidInputException("Failed to resolve role: " + backendRole);
        }

        // 5. Build and persist the user with all captured fields.
        AppUser user = AppUser.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .dateOfBirth(req.getDateOfBirth())
                .gender(mapGender(req.getGender()))
                .profilePicUrl(blankToNull(req.getProfilePic()))
                .employeeId(blankToNull(req.getEmployeeId()))
                .role(backendRole)
                .roleEntity(roleEntity)
                .kycStatus("VERIFIED") // admin-created accounts are pre-verified (matches self-registration)
                .community(community)
                .block(blankToNull(req.getBlock()))
                .tower(blankToNull(req.getTower()))
                .flatNo(blankToNull(req.getFlatNo()))
                .residentType(blankToNull(req.getResidentType()))
                .occupancyStatus(blankToNull(req.getOccupancyStatus()))
                .isActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive())
                .notifyEmail(orDefault(req.getPrefEmail(), true))
                .notifySms(orDefault(req.getPrefSms(), false))
                .notifyWhatsapp(orDefault(req.getPrefWhatsapp(), true))
                .notifyPush(orDefault(req.getPrefPush(), true))
                .build();

        AppUser saved = userRepository.save(user);

        // 6. Copy the role's permission templates onto user-specific rows so the
        //    new account actually carries its permissions (mirrors updateUserRole).
        assignRolePermissions(saved, backendRole, roleEntity);

        auditLog.record(AuditLogService.Action.REGISTER, saved.getId(), saved.getEmail());
        return saved;
    }

    private Community resolveCommunity(AdminCreateUserRequest req) {
        if (req.getCommunityId() != null) {
            return communityRepository.findById(req.getCommunityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId()));
        }
        if (req.getInviteCode() != null && !req.getInviteCode().isBlank()) {
            return communityRepository.findByInviteCode(req.getInviteCode().trim())
                    .orElseThrow(() -> new InvalidInviteCodeException(req.getInviteCode()));
        }
        throw new InvalidInputException("A community is required (communityId or inviteCode).");
    }

    /** Copies generic role permission templates to user-specific rows (see UserController.updateUserRole). */
    private void assignRolePermissions(AppUser user, String role, Role roleEntity) {
        rolePermissionRepo.deleteByUserId(user.getId());
        rolePermissionRepo.flush();

        Set<RolePermission> templates =
                roleEntity.getPermissions() != null ? roleEntity.getPermissions() : Collections.emptySet();

        List<RolePermission> userPermissions = templates.stream()
                .filter(t -> t.getUser() == null) // copy from the generic template rows
                .map(RolePermission::getPermissionKey)
                .filter(pk -> pk != null && !pk.trim().isEmpty())
                .distinct()
                .map(pk -> RolePermission.builder()
                        .role(role)
                        .roleEntity(roleEntity)
                        .permissionKey(pk)
                        .user(user)
                        .build())
                .toList();

        if (!userPermissions.isEmpty()) {
            rolePermissionRepo.saveAll(userPermissions);
        }
    }

    /** Maps a UI role (admin/committee/resident/security/vendor/staff) to a backend role. */
    private String mapRole(String uiRole) {
        if (uiRole == null || uiRole.isBlank()) {
            return ROLE_MEMBER;
        }
        String r = uiRole.trim();
        // "resident" is the standard member role in this system.
        if (r.equalsIgnoreCase("resident") || r.equalsIgnoreCase("member")) {
            return ROLE_MEMBER;
        }
        return r.toUpperCase();
    }

    /** Maps a gender label from the form to the stored code (MALE/FEMALE/OTHER). */
    private String mapGender(String label) {
        if (label == null) return "OTHER";
        String g = label.trim().toLowerCase();
        if (g.startsWith("male")) return "MALE";
        if (g.startsWith("female")) return "FEMALE";
        return "OTHER";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static Boolean orDefault(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }
}

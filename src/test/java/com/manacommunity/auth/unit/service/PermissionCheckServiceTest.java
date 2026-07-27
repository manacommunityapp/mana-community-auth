package com.manacommunity.auth.unit.service;

import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.model.RolePermission;
import com.manacommunity.common.repository.RolePermissionRepository;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.auth.user.service.LoggedInUserService;
import com.manacommunity.auth.service.PermissionCheckService;
import com.manacommunity.auth.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionCheckService")
class PermissionCheckServiceTest {

    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock LoggedInUserService loggedInUserService;

    @InjectMocks PermissionCheckService permissionCheckService;

    private UserPrincipal principal(AppUser user) {
        return new UserPrincipal(user.getId(), user.getEmail(), "pw",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }

    @Nested
    @DisplayName("hasAnyPermission")
    class HasAny {

        @Test
        @DisplayName("SUPER_ADMIN always returns true without hitting DB")
        void superAdminBypass() {
            AppUser superAdmin = TestDataBuilder.superAdmin();
            when(loggedInUserService.resolve(any())).thenReturn(superAdmin);

            boolean result = permissionCheckService.hasAnyPermission(
                    principal(superAdmin), "VIEW_SPORTS_MAIN");

            assertThat(result).isTrue();
            verifyNoInteractions(rolePermissionRepository);
        }

        @Test
        @DisplayName("user with matching user-specific permission returns true")
        void userSpecificPermission() {
            AppUser admin = TestDataBuilder.adminUser();
            Role role = TestDataBuilder.adminRole();
            RolePermission rp = TestDataBuilder.rolePermission(role, "VIEW_SPORTS_MAIN");
            rp.setUser(admin);

            when(loggedInUserService.resolve(any())).thenReturn(admin);
            when(rolePermissionRepository.findByUserId(admin.getId())).thenReturn(List.of(rp));

            assertThat(permissionCheckService.hasAnyPermission(
                    principal(admin), "VIEW_SPORTS_MAIN")).isTrue();
        }

        @Test
        @DisplayName("user with no matching permission returns false")
        void noMatchingPermission() {
            AppUser member = TestDataBuilder.memberUser();
            when(loggedInUserService.resolve(any())).thenReturn(member);
            when(rolePermissionRepository.findByUserId(member.getId())).thenReturn(List.of());
            when(rolePermissionRepository.findByRoleIgnoreCase("MEMBER")).thenReturn(List.of());

            assertThat(permissionCheckService.hasAnyPermission(
                    principal(member), "CREATE_EDIT_SPORTS_MAIN")).isFalse();
        }

        @Test
        @DisplayName("role-based permission used as fallback when no user-specific rows exist")
        void roleBasedFallback() {
            AppUser member = TestDataBuilder.memberUser();
            Role role = TestDataBuilder.memberRole();
            RolePermission rp = TestDataBuilder.rolePermission(role, "VIEW_SPORTS_MAIN");

            when(loggedInUserService.resolve(any())).thenReturn(member);
            when(rolePermissionRepository.findByUserId(member.getId())).thenReturn(List.of());
            when(rolePermissionRepository.findByRoleIgnoreCase("MEMBER")).thenReturn(List.of(rp));

            assertThat(permissionCheckService.hasAnyPermission(
                    principal(member), "VIEW_SPORTS_MAIN")).isTrue();
        }
    }

    @Nested
    @DisplayName("requireAnyPermission")
    class RequireAny {

        @Test
        @DisplayName("throws AccessDeniedException when permission is missing")
        void throwsWhenMissing() {
            AppUser member = TestDataBuilder.memberUser();
            when(loggedInUserService.resolve(any())).thenReturn(member);
            when(rolePermissionRepository.findByUserId(anyLong())).thenReturn(List.of());
            when(rolePermissionRepository.findByRoleIgnoreCase(anyString())).thenReturn(List.of());

            assertThatThrownBy(() ->
                    permissionCheckService.requireAnyPermission(
                            principal(member), "CREATE_EDIT_SPORTS_MAIN"))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("CREATE_EDIT_SPORTS_MAIN");
        }

        @Test
        @DisplayName("does not throw when user has any of the required permissions")
        void doesNotThrowWhenGranted() {
            AppUser admin = TestDataBuilder.adminUser();
            Role role = TestDataBuilder.adminRole();
            RolePermission rp = TestDataBuilder.rolePermission(role, "CREATE_EDIT_SPORTS_MAIN");

            when(loggedInUserService.resolve(any())).thenReturn(admin);
            when(rolePermissionRepository.findByUserId(admin.getId())).thenReturn(List.of(rp));

            assertThatCode(() ->
                    permissionCheckService.requireAnyPermission(
                            principal(admin), "CREATE_EDIT_SPORTS_MAIN", "VIEW_SPORTS_MAIN"))
                    .doesNotThrowAnyException();
        }
    }
}

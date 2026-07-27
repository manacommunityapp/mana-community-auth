package com.manacommunity.auth.unit.service;

import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.model.RolePermission;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.common.repository.RolePermissionRepository;
import com.manacommunity.auth.repository.RoleRepository;
import com.manacommunity.auth.service.impl.RolePermissionServiceImpl;
import com.manacommunity.auth.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolePermissionService")
class RolePermissionServiceTest {

    @Mock RoleRepository roleRepo;
    @Mock RolePermissionRepository rolePermissionRepo;
    @Mock AppUserRepository appUserRepo;

    @InjectMocks RolePermissionServiceImpl service;

    @Nested
    @DisplayName("updateRolePermissions")
    class UpdateRole {

        @Test
        @DisplayName("clears old permissions and saves new ones")
        void replacesPermissions() {
            Role role = TestDataBuilder.adminRole();
            when(roleRepo.findByNameIgnoreCaseAndCommunityId("ADMIN", 1L))
                    .thenReturn(Optional.of(role));

            service.updateRolePermissions("ADMIN", 1L, List.of("VIEW_SPORTS_MAIN", "CREATE_EDIT_SPORTS_MAIN"));

            verify(rolePermissionRepo).deleteByRoleEntityIdAndUserIsNull(role.getId());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("blank permission strings are filtered out")
        void filtersBlankPermissions() {
            Role role = TestDataBuilder.adminRole();
            when(roleRepo.findByNameIgnoreCaseAndCommunityId(anyString(), anyLong()))
                    .thenReturn(Optional.of(role));

            service.updateRolePermissions("ADMIN", 1L, List.of("VIEW_SPORTS_MAIN", "", "  "));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("duplicate permissions are deduplicated before save")
        void deduplicates() {
            Role role = TestDataBuilder.adminRole();
            when(roleRepo.findByNameIgnoreCaseAndCommunityId(anyString(), anyLong()))
                    .thenReturn(Optional.of(role));

            service.updateRolePermissions("ADMIN", 1L,
                    List.of("VIEW_SPORTS_MAIN", "VIEW_SPORTS_MAIN", "CREATE_EDIT_SPORTS_MAIN"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepo).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown role")
        void unknownRole() {
            when(roleRepo.findByNameIgnoreCaseAndCommunityId(anyString(), anyLong()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updateRolePermissions("UNKNOWN", 1L, List.of("VIEW_SPORTS_MAIN")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateUserPermissions")
    class UpdateUser {

        @Test
        @DisplayName("clears old user permissions and saves new ones")
        void replacesUserPermissions() {
            AppUser user = TestDataBuilder.memberUser();
            when(appUserRepo.findById(user.getId())).thenReturn(Optional.of(user));

            service.updateUserPermissions(user.getId(), "MEMBER",
                    List.of("VIEW_SPORTS_MAIN", "VIEW_EVENT_REGISTRATIONS"));

            verify(rolePermissionRepo).deleteByUserId(user.getId());
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepo).saveAll(captor.capture());
            assertThat(captor.getValue())
                    .allMatch(rp -> rp.getUser() != null && rp.getUser().getId().equals(user.getId()));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for unknown user")
        void unknownUser() {
            when(appUserRepo.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updateUserPermissions(999L, "MEMBER", List.of("VIEW_SPORTS_MAIN")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

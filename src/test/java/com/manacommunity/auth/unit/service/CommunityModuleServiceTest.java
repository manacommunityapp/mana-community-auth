package com.manacommunity.auth.unit.service;

import com.manacommunity.auth.dto.CommunityModuleBulkRequest;
import com.manacommunity.auth.dto.CommunityModuleResponse;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.model.CommunityModule;
import com.manacommunity.auth.repository.CommunityModuleRepository;
import com.manacommunity.auth.repository.CommunityRepository;
import com.manacommunity.auth.service.impl.CommunityModuleServiceImpl;
import com.manacommunity.auth.support.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityModuleService")
class CommunityModuleServiceTest {

    @Mock CommunityModuleRepository communityModuleRepository;
    @Mock CommunityRepository communityRepository;

    @InjectMocks CommunityModuleServiceImpl communityModuleService;

    @Test
    @DisplayName("initializeModulesForCommunity sets COMMUNITY_FEED and ADMIN_HUB to true, others to false, with explicit insertion timestamps")
    void initializeModulesForCommunityTest() {
        Community community = TestDataBuilder.community();
        when(communityModuleRepository.existsByCommunityId(1L)).thenReturn(false);
        when(communityRepository.findById(1L)).thenReturn(Optional.of(community));

        communityModuleService.initializeModulesForCommunity(1L);

        ArgumentCaptor<List<CommunityModule>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(communityModuleRepository).saveAll(listCaptor.capture());

        List<CommunityModule> savedModules = listCaptor.getValue();
        assertThat(savedModules).isNotEmpty();

        for (CommunityModule cm : savedModules) {
            assertThat(cm.getCommunity()).isEqualTo(community);
            assertThat(cm.getCreatedAt()).isNotNull();
            assertThat(cm.getUpdatedAt()).isNotNull();

            if (cm.getModuleKey().equals("COMMUNITY_FEED") || cm.getModuleKey().equals("ADMIN_HUB")) {
                assertThat(cm.getIsEnabled()).isTrue();
            } else {
                assertThat(cm.getIsEnabled()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("isModuleEnabled returns false by default if community modules do not exist")
    void isModuleEnabledDefaultFalse() {
        when(communityModuleRepository.existsByCommunityId(1L)).thenReturn(false);

        boolean enabled = communityModuleService.isModuleEnabled(1L, "SPORTS");

        assertThat(enabled).isFalse();
        verify(communityModuleRepository, never()).isModuleEnabled(anyLong(), anyString());
    }

    @Test
    @DisplayName("isModuleEnabled delegates to repository when community modules exist")
    void isModuleEnabledDelegates() {
        when(communityModuleRepository.existsByCommunityId(1L)).thenReturn(true);
        when(communityModuleRepository.isModuleEnabled(1L, "SPORTS")).thenReturn(true);

        boolean enabled = communityModuleService.isModuleEnabled(1L, "SPORTS");

        assertThat(enabled).isTrue();
    }
}

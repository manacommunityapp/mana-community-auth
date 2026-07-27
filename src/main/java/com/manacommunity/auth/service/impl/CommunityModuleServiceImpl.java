package com.manacommunity.auth.service.impl;

import com.manacommunity.common.constants.ModuleConstants;
import com.manacommunity.auth.dto.CommunityModuleBulkRequest;
import com.manacommunity.auth.dto.CommunityModuleResponse;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.model.CommunityModule;
import com.manacommunity.auth.repository.CommunityModuleRepository;
import com.manacommunity.auth.repository.CommunityRepository;
import com.manacommunity.auth.service.CommunityModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityModuleServiceImpl implements CommunityModuleService {

    private final CommunityModuleRepository communityModuleRepo;
    private final CommunityRepository communityRepo;

    @Override
    @Transactional(readOnly = true)
    public List<CommunityModuleResponse> getModulesForCommunity(Long communityId) {
        return communityModuleRepo.findByCommunityIdOrderBySortOrderAsc(communityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getEnabledModuleKeys(Long communityId) {
        if (!communityModuleRepo.existsByCommunityId(communityId)) {
            return List.of();
        }
        return communityModuleRepo.findEnabledByCommunityId(communityId).stream()
                .map(CommunityModule::getModuleKey)
                .toList();
    }

    @Override
    @Transactional
    public CommunityModuleResponse toggleModule(Long communityId, String moduleKey, boolean enabled) {
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        CommunityModule cm = communityModuleRepo.findByCommunityIdAndModuleKey(communityId, moduleKey)
                .orElseGet(() -> {
                    com.manacommunity.common.constants.ModuleConstants.ModuleDef def = com.manacommunity.common.constants.ModuleConstants.ALL_MODULES.stream()
                            .filter(m -> m.key().equals(moduleKey))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("CommunityModule", "moduleKey", moduleKey));
                    LocalDateTime now = LocalDateTime.now();
                    return CommunityModule.builder()
                            .community(community)
                            .moduleKey(def.key())
                            .moduleLabel(def.label())
                            .isEnabled(def.key().equals("COMMUNITY_FEED") || def.key().equals("ADMIN_HUB"))
                            .sortOrder(def.sortOrder())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                });

        cm.setIsEnabled(enabled);
        return toResponse(communityModuleRepo.save(cm));
    }

    @Override
    @Transactional
    public List<CommunityModuleResponse> bulkUpdate(CommunityModuleBulkRequest request) {
        Community community = communityRepo.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", request.communityId()));

        List<CommunityModuleResponse> results = request.modules().stream().map(toggle -> {
            CommunityModule cm = communityModuleRepo
                    .findByCommunityIdAndModuleKey(request.communityId(), toggle.moduleKey())
                    .orElseGet(() -> {
                        com.manacommunity.common.constants.ModuleConstants.ModuleDef def = com.manacommunity.common.constants.ModuleConstants.ALL_MODULES.stream()
                                .filter(m -> m.key().equals(toggle.moduleKey()))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException("CommunityModule", "moduleKey", toggle.moduleKey()));
                        LocalDateTime now = LocalDateTime.now();
                        return CommunityModule.builder()
                                .community(community)
                                .moduleKey(def.key())
                                .moduleLabel(def.label())
                                .isEnabled(def.key().equals("COMMUNITY_FEED") || def.key().equals("ADMIN_HUB"))
                                .sortOrder(def.sortOrder())
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                    });
            cm.setIsEnabled(toggle.isEnabled());
            return toResponse(communityModuleRepo.save(cm));
        }).toList();

        log.info("Bulk-updated {} modules for community {}", results.size(), request.communityId());
        return results;
    }

    @Override
    @Transactional
    public void initializeModulesForCommunity(Long communityId) {
        if (communityModuleRepo.existsByCommunityId(communityId)) {
            return;
        }

        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        LocalDateTime now = LocalDateTime.now();
        List<CommunityModule> modules = ModuleConstants.ALL_MODULES.stream()
                .map(def -> CommunityModule.builder()
                        .community(community)
                        .moduleKey(def.key())
                        .moduleLabel(def.label())
                        .isEnabled(def.key().equals("COMMUNITY_FEED") || def.key().equals("ADMIN_HUB"))
                        .sortOrder(def.sortOrder())
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        communityModuleRepo.saveAll(modules);
        log.info("Initialized {} modules for community '{}'", modules.size(), community.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleEnabled(Long communityId, String moduleKey) {
        if (!communityModuleRepo.existsByCommunityId(communityId)) {
            return false;
        }
        return communityModuleRepo.isModuleEnabled(communityId, moduleKey);
    }

    private CommunityModuleResponse toResponse(CommunityModule cm) {
        return new CommunityModuleResponse(
                cm.getId(),
                cm.getCommunity().getId(),
                cm.getModuleKey(),
                cm.getModuleLabel(),
                cm.getIsEnabled(),
                cm.getSortOrder()
        );
    }
}

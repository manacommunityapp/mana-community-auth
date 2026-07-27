package com.manacommunity.auth.service.impl;

import com.manacommunity.common.model.Community;
import com.manacommunity.common.model.CommunityModule;
import com.manacommunity.auth.repository.CommunityModuleRepository;
import com.manacommunity.auth.repository.CommunityRepository;
import com.manacommunity.common.dto.CommunityResponse;
import com.manacommunity.auth.service.CommunityModuleService;
import com.manacommunity.auth.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityServiceImpl implements CommunityService {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityRoleInitializer communityRoleInitializer;

    @Autowired
    private CommunityModuleService communityModuleService;

    @Autowired
    private CommunityModuleRepository communityModuleRepo;

    @Override
    public List<CommunityResponse> getAllCommunities() {
        return communityRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toPublicResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommunityResponse> getCommunitiesByType(String type) {
        return communityRepository.findByActiveTrueAndTypeIgnoreCaseOrderByNameAsc(type)
                .stream()
                .map(this::toPublicResponse)
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CommunityResponse createCommunity(CommunityResponse request) {
        Community community = toEntity(request);
        community.setActive(true);
        Community saved = communityRepository.save(community);
        communityRoleInitializer.initializeCommunityRoles(saved);
        communityModuleService.initializeModulesForCommunity(saved.getId());
        return toResponse(saved);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CommunityResponse updateCommunity(Long id, CommunityResponse request) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("Community", id));
        community.setName(request.getName());
        community.setType(request.getType());
        community.setCity(request.getCity());
        community.setState(request.getState());
        community.setArea(request.getArea());
        community.setSubtype(request.getSubtype());
        community.setInviteCode(request.getInviteCode());
        return toResponse(communityRepository.save(community));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CommunityResponse updateEnabledModules(Long id, List<String> modules) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("Community", id));

        var allModules = communityModuleRepo.findByCommunityIdOrderBySortOrderAsc(id);
        for (CommunityModule cm : allModules) {
            boolean shouldEnable = modules != null && modules.contains(cm.getModuleKey());
            cm.setIsEnabled(shouldEnable);
        }
        communityModuleRepo.saveAll(allModules);

        return toResponse(community);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteCommunity(Long id) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("Community", id));
        community.setActive(false);
        communityRepository.save(community);
    }

    private Community toEntity(CommunityResponse r) {
        return Community.builder()
                .name(r.getName())
                .type(r.getType())
                .city(r.getCity())
                .state(r.getState())
                .area(r.getArea())
                .subtype(r.getSubtype())
                .inviteCode(r.getInviteCode())
                .build();
    }

    private CommunityResponse toPublicResponse(Community c) {
        List<String> modules = communityModuleService.getEnabledModuleKeys(c.getId());
        return new CommunityResponse(
                c.getId(),
                c.getName(),
                c.getType(),
                c.getCity(),
                c.getState(),
                c.getArea(),
                c.getSubtype(),
                null,
                c.getActive(),
                modules
        );
    }

    private CommunityResponse toResponse(Community c) {
        List<String> modules = communityModuleService.getEnabledModuleKeys(c.getId());
        return new CommunityResponse(
                c.getId(),
                c.getName(),
                c.getType(),
                c.getCity(),
                c.getState(),
                c.getArea(),
                c.getSubtype(),
                c.getInviteCode(),
                c.getActive(),
                modules
        );
    }
}

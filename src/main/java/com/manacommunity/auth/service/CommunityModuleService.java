package com.manacommunity.auth.service;

import com.manacommunity.auth.dto.CommunityModuleBulkRequest;
import com.manacommunity.auth.dto.CommunityModuleResponse;

import java.util.List;

public interface CommunityModuleService {

    List<CommunityModuleResponse> getModulesForCommunity(Long communityId);

    List<String> getEnabledModuleKeys(Long communityId);

    CommunityModuleResponse toggleModule(Long communityId, String moduleKey, boolean enabled);

    List<CommunityModuleResponse> bulkUpdate(CommunityModuleBulkRequest request);

    void initializeModulesForCommunity(Long communityId);

    boolean isModuleEnabled(Long communityId, String moduleKey);
}

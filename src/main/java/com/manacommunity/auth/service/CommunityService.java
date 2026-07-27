package com.manacommunity.auth.service;

import com.manacommunity.common.dto.CommunityResponse;

import java.util.List;

public interface CommunityService {

    /** Returns all active communities for the signup dropdown. */
    List<CommunityResponse> getAllCommunities();

    /** Returns communities filtered by type (APARTMENT, COLLEGE, etc.). */
    List<CommunityResponse> getCommunitiesByType(String type);

    /** Creates a new community. */
    CommunityResponse createCommunity(CommunityResponse request);

    /** Updates an existing community's editable details. */
    CommunityResponse updateCommunity(Long id, CommunityResponse request);

    /** Soft-deletes a community (sets active=false; the row is preserved). */
    void deleteCommunity(Long id);

    /** Updates the list of enabled feature modules for a community. */
    CommunityResponse updateEnabledModules(Long id, List<String> modules);
}

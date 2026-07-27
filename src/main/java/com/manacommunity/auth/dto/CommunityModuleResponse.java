package com.manacommunity.auth.dto;

public record CommunityModuleResponse(
    Long id,
    Long communityId,
    String moduleKey,
    String moduleLabel,
    Boolean isEnabled,
    Integer sortOrder
) {}

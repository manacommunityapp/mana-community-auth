package com.manacommunity.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CommunityModuleBulkRequest(
    @NotNull Long communityId,
    @NotNull List<ModuleToggle> modules
) {
    public record ModuleToggle(
        @NotNull String moduleKey,
        @NotNull Boolean isEnabled
    ) {}
}

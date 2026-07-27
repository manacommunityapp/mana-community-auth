package com.manacommunity.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuItemRequest(
    @NotBlank @Size(max = 50) String menuKey,
    @NotBlank @Size(max = 100) String label,
    @Size(max = 50) String icon,
    @Size(max = 255) String routePath,
    Long parentId,
    Integer sortOrder,
    Boolean isActive,
    @Size(max = 100) String permissionKey,
    Long communityId
) {}

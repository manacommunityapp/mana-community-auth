package com.manacommunity.auth.controller;

import com.manacommunity.common.model.Community;

import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.dto.CommunityResponse;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.auth.service.CommunityService;
import com.manacommunity.auth.user.service.LoggedInUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Community endpoints.
 * GET is public (signup dropdown). POST requires authentication.
 */
@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private LoggedInUserService loggedInUserService;

    /**
     * GET /api/communities
     * Returns all communities for the signup dropdown.
     * Optional ?type= filter (APARTMENT | COLLEGE | SCHOOL | OFFICE)
     *
     * Example:
     *   GET /api/communities          → all communities
     *   GET /api/communities?type=APARTMENT → only apartments
     */
    @GetMapping
    public ResponseEntity<List<CommunityResponse>> getCommunities(
            @RequestParam(required = false) String type) {

        List<CommunityResponse> result = (type != null && !type.isBlank())
                ? communityService.getCommunitiesByType(type)
                : communityService.getAllCommunities();

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/communities
     * Creates a new community. Restricted to ADMIN and SUPER_ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @Valid @RequestBody CommunityResponse request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(communityService.createCommunity(request));
    }

    /**
     * PUT /api/communities/{id}
     * Updates an existing community. Restricted to ADMIN and SUPER_ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CommunityResponse> updateCommunity(
            @PathVariable Long id,
            @Valid @RequestBody CommunityResponse request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(communityService.updateCommunity(id, request));
    }

    /**
     * DELETE /api/communities/{id}
     * Soft-deletes a community (sets active=false). Restricted to ADMIN and SUPER_ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommunity(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        communityService.deleteCommunity(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/communities/{id}/modules
     * Updates enabled feature modules for a community. SUPER_ADMIN only.
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/modules")
    public ResponseEntity<CommunityResponse> updateModules(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, java.util.List<String>> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(communityService.updateEnabledModules(id, body.get("modules")));
    }
}

package com.manacommunity.auth.controller;

import com.manacommunity.auth.dto.CommunityModuleBulkRequest;
import com.manacommunity.auth.dto.CommunityModuleResponse;
import com.manacommunity.auth.service.CommunityModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community-modules")
@RequiredArgsConstructor
public class CommunityModuleController {

    private final CommunityModuleService communityModuleService;

    @GetMapping("/{communityId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<List<CommunityModuleResponse>> getModules(@PathVariable Long communityId) {
        return ResponseEntity.ok(communityModuleService.getModulesForCommunity(communityId));
    }

    @GetMapping("/{communityId}/keys")
    public ResponseEntity<List<String>> getEnabledKeys(@PathVariable Long communityId) {
        return ResponseEntity.ok(communityModuleService.getEnabledModuleKeys(communityId));
    }

    @PutMapping("/{communityId}/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CommunityModuleResponse> toggleModule(
            @PathVariable Long communityId,
            @RequestBody Map<String, Object> body) {
        String moduleKey = (String) body.get("moduleKey");
        boolean enabled = (Boolean) body.getOrDefault("enabled", true);
        return ResponseEntity.ok(communityModuleService.toggleModule(communityId, moduleKey, enabled));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CommunityModuleResponse>> bulkUpdate(
            @Valid @RequestBody CommunityModuleBulkRequest request) {
        return ResponseEntity.ok(communityModuleService.bulkUpdate(request));
    }

    @PostMapping("/{communityId}/initialize")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> initializeModules(@PathVariable Long communityId) {
        communityModuleService.initializeModulesForCommunity(communityId);
        return ResponseEntity.ok().build();
    }
}

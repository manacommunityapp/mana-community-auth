package com.manacommunity.auth.user.controller;

import com.manacommunity.auth.user.dto.UserProfileRequest;
import com.manacommunity.auth.user.dto.UserProfileResponse;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.security.UserPrincipal;
import com.manacommunity.auth.user.service.LoggedInUserService;
import com.manacommunity.auth.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final LoggedInUserService loggedInUserService;
    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        UserProfileResponse response = userProfileService.getProfile(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserProfileRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        UserProfileResponse response = userProfileService.updateProfile(user, request);
        return ResponseEntity.ok(response);
    }
}

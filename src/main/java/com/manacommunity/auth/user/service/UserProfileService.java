package com.manacommunity.auth.user.service;

import com.manacommunity.auth.user.dto.UserProfileRequest;
import com.manacommunity.auth.user.dto.UserProfileResponse;
import com.manacommunity.common.user.model.AppUser;

public interface UserProfileService {
    UserProfileResponse getProfile(AppUser user);
    UserProfileResponse updateProfile(AppUser user, UserProfileRequest request);
}

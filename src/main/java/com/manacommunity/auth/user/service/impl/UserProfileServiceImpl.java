package com.manacommunity.auth.user.service.impl;

import com.manacommunity.common.exception.DuplicateResourceException;
import com.manacommunity.auth.user.dto.UserProfileRequest;
import com.manacommunity.auth.user.dto.UserProfileResponse;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.auth.user.model.UserProfile;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.auth.user.repository.UserProfileRepository;
import com.manacommunity.auth.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public UserProfileResponse getProfile(AppUser user) {
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .user(user)
                            .bio("Software Engineer | Sports enthusiast | Community member.")
                            .skills("Cricket,Community Building,Badminton")
                            .posts(15)
                            .connections(52)
                            .eventsAttended(4)
                            .itemsSold(2)
                            .jobsPosted(1)
                            .sportsPlayed(3)
                            .build();
                    return userProfileRepository.save(newProfile);
                });

        return mapToResponse(user, profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(AppUser user, UserProfileRequest request) {
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (appUserRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (appUserRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateResourceException("User", "phone", request.getPhone());
            }
            user.setPhone(request.getPhone());
        }
        if (request.getDob() != null) user.setDateOfBirth(request.getDob());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getFlatNo() != null) user.setFlatNo(request.getFlatNo());
        if (request.getBlock() != null) user.setBlock(request.getBlock());
        if (request.getProfilePicUrl() != null) user.setProfilePicUrl(request.getProfilePicUrl());
        appUserRepository.save(user);

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getCoverPicUrl() != null) profile.setCoverPicUrl(request.getCoverPicUrl());
        if (request.getSkills() != null) {
            String skillsStr = request.getSkills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.joining(","));
            profile.setSkills(skillsStr);
        }

        userProfileRepository.save(profile);

        return mapToResponse(user, profile);
    }

    private UserProfileResponse mapToResponse(AppUser user, UserProfile profile) {
        List<String> skillsList = Collections.emptyList();
        if (profile.getSkills() != null && !profile.getSkills().trim().isEmpty()) {
            skillsList = Arrays.stream(profile.getSkills().split(","))
                    .map(String::trim)
                    .toList();
        }

        String joinedAtStr = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "";

        // Sports stats (eventsAttended, sportsPlayed) will be fetched via
        // cross-service API call to sports-service in a future iteration.
        UserProfileResponse.UserStats stats = UserProfileResponse.UserStats.builder()
                .posts(profile.getPosts() != null ? profile.getPosts() : 0)
                .connections(profile.getConnections() != null ? profile.getConnections() : 0)
                .eventsAttended(profile.getEventsAttended() != null ? profile.getEventsAttended() : 0)
                .itemsSold(profile.getItemsSold() != null ? profile.getItemsSold() : 0)
                .jobsPosted(profile.getJobsPosted() != null ? profile.getJobsPosted() : 0)
                .sportsPlayed(profile.getSportsPlayed() != null ? profile.getSportsPlayed() : 0)
                .build();

        return UserProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .dob(user.getDateOfBirth())
                .gender(user.getGender())
                .flatNo(user.getFlatNo())
                .block(user.getBlock())
                .role(user.getRole())
                .kycStatus(user.getKycStatus())
                .communityName(user.getCommunity() != null ? user.getCommunity().getName() : "")
                .communityType(user.getCommunity() != null ? user.getCommunity().getType() : "")
                .communityCode(user.getCommunity() != null ? user.getCommunity().getInviteCode() : "")
                .joinedAt(joinedAtStr)
                .bio(profile.getBio())
                .profilePicUrl(user.getProfilePicUrl())
                .coverPicUrl(profile.getCoverPicUrl())
                .skills(skillsList)
                .stats(stats)
                .build();
    }
}

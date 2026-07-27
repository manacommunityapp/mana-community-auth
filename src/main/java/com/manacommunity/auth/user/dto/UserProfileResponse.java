package com.manacommunity.auth.user.dto;

import com.manacommunity.common.model.Community;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String flatNo;
    private String block;
    private String role;
    private String kycStatus;
    
    // Community details
    private String communityName;
    private String communityType;
    private String communityCode;
    private String joinedAt; // Format: yyyy-MM-dd
    
    // Profile details
    private String bio;
    private String profilePicUrl;
    private String coverPicUrl;
    private List<String> skills;
    
    // Stats
    private UserStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private int posts;
        private int connections;
        private int eventsAttended;
        private int itemsSold;
        private int jobsPosted;
        private int sportsPlayed;
    }
}

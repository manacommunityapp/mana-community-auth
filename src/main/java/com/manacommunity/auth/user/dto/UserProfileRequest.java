package com.manacommunity.auth.user.dto;

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
public class UserProfileRequest {
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dob;
    private String gender;
    private String flatNo;
    private String block;
    private String bio;
    private List<String> skills;
    private String profilePicUrl;
    private String coverPicUrl;
}

package com.manacommunity.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

/**
 * Payload for the admin "Create New User" page. Mirrors the fields the form
 * collects; the service maps them onto {@link com.manacommunity.common.user.model.AppUser}
 * (first+last name -> fullName, UI role -> backend role, gender label -> code, etc.).
 *
 * A community must be identifiable either by {@code communityId} (preferred) or
 * {@code inviteCode}; the service rejects the request if neither resolves.
 */
@Data
public class AdminCreateUserRequest {

    // ── Personal information ──
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String email;
    @NotBlank
    private String phone;
    @NotNull
    @Past
    private LocalDate dateOfBirth;
    @NotBlank
    private String gender;              // UI label: Male / Female / Other / Prefer not to say

    private String profilePic;          // URL or inline base64 data-URI
    private String employeeId;          // external Resident/Employee ID
    private Boolean isActive;           // account access; defaults to true

    // ── Community link ──
    private Long communityId;           // preferred way to resolve the target community
    private String inviteCode;          // fallback if communityId is absent

    // ── Community unit details ──
    private String block;
    private String tower;
    private String flatNo;
    private String residentType;        // Resident / Non-Resident / Guest
    private String occupancyStatus;     // Owner / Tenant / Staff

    // ── Account & role ──
    private String password;            // optional; a temporary password is set when blank
    private String role;                // UI role: admin/committee/resident/security/vendor/staff

    // ── Notification preferences ──
    private Boolean prefEmail;
    private Boolean prefSms;
    private Boolean prefWhatsapp;
    private Boolean prefPush;
}

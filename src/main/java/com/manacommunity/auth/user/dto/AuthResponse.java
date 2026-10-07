package com.manacommunity.auth.user.dto;

public class AuthResponse {
    private String userId;
    private String message;
    private String token; // short-lived JWT access token
    private String refreshToken; // long-lived JWT refresh token (exchanged at /api/auth/refresh)

    // User details for frontend context
    private String fullName;
    private String email;
    private String role;
    private Long communityId;
    private java.time.LocalDate dateOfBirth;
    private java.util.List<String> enabledModules;

    private boolean mfaRequired;
    private String preAuthToken;

    public AuthResponse() {}

    public AuthResponse(String userId, String message, String token) {
        this.userId = userId;
        this.message = message;
        this.token = token;
    }

    public AuthResponse(String userId, String message, String token, String fullName, String email, String role, Long communityId, java.time.LocalDate dateOfBirth) {
        this.userId = userId;
        this.message = message;
        this.token = token;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.communityId = communityId;
        this.dateOfBirth = dateOfBirth;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }

    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public java.util.List<String> getEnabledModules() { return enabledModules; }
    public void setEnabledModules(java.util.List<String> enabledModules) { this.enabledModules = enabledModules; }

    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }

    public String getPreAuthToken() { return preAuthToken; }
    public void setPreAuthToken(String preAuthToken) { this.preAuthToken = preAuthToken; }
}

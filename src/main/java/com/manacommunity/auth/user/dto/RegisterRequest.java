package com.manacommunity.auth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

/**
 * BUG FIX: RegisterRequest was a plain class with no accessor methods.
 * SportsEventServiceImpl uses record-style accessors (req.dateOfBirth();
 * req.gender()) — converted to a Java record.
 *
 * Also added required fields missing from the original:
 * - dateOfBirth (NOT NULL in app_user table)
 * - gender (NOT NULL in app_user table)
 */
@Data
public class RegisterRequest {
    @NotBlank
    String fullName;
    @NotBlank
    String email;
    @NotBlank
    String phone;
    String aadharNumber;
    @NotBlank
    String inviteCode;
    @NotBlank
    String password;
    @NotNull
    @Past
    LocalDate dateOfBirth; // BUG FIX: required by app_user schema
    @NotBlank
    String gender; // BUG FIX: required by app_user schema (MALE/FEMALE/OTHER)
    @NotBlank
    String flatNo;
    String block;
}

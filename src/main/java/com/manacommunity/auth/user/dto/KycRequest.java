package com.manacommunity.auth.user.dto;

import com.manacommunity.auth.user.service.AuthService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  KycRequest
 *  ─────────────────────────────────────────────────────────────────
 *  Inbound DTO received by:
 *    POST /api/auth/verify-kyc
 *    → AuthController.verifyKyc(@RequestBody KycRequest req, ...)
 *    → AuthService.submitKyc(Long userId, KycRequest req)
 *
 *  Fields derived from:
 *    1. AuthController  — @RequestBody KycRequest req
 *    2. app_user table  — govt_id_type, govt_id_number_enc
 *    3. kyc_document    — doc_type, s3_key, user_id
 *    4. DB constraints  — CHECK (govt_id_type IN ('AADHAAR','VOTER_ID','DRIVING_LICENCE'))
 * ═══════════════════════════════════════════════════════════════════
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycRequest {

    // ── Govt ID fields (maps to app_user.govt_id_type) ────────────

    /**
     * Type of government-issued identity document.
     * Allowed values: AADHAAR | VOTER_ID | DRIVING_LICENCE
     * Maps to: app_user.govt_id_type (VARCHAR 20)
     *          CHECK constraint: IN ('AADHAAR','VOTER_ID','DRIVING_LICENCE')
     */
    @NotNull(message = "Government ID type is required")
    private GovtIdType govtIdType;

    /**
     * The actual government ID number (plain text from the user).
     * Stored encrypted in app_user.govt_id_number_enc (AES-256).
     * Validation patterns enforced per ID type in AuthService.
     *
     * Aadhaar        : 12-digit numeric  (e.g. 1234 5678 9012)
     * Voter ID       : alphanumeric 10   (e.g. ABC1234567)
     * Driving Licence: state-code + digits (e.g. DL0420110123456)
     */
    @NotBlank(message = "Government ID number is required")
    @Size(min = 8, max = 20, message = "Government ID number must be 8–20 characters")
    private String govtIdNumber;

    // ── Document upload fields (maps to kyc_document table) ───────

    /**
     * Document type label stored in kyc_document.doc_type.
     * Typically mirrors govtIdType but allows FRONT / BACK suffixes.
     * Examples: "AADHAAR_FRONT", "AADHAAR_BACK", "VOTER_ID", "DRIVING_LICENCE"
     * Maps to: kyc_document.doc_type (VARCHAR 20)
     */
    @NotBlank(message = "Document type is required")
    @Size(max = 20, message = "Document type must not exceed 20 characters")
    private String docType;

    /**
     * S3 object key of the uploaded document scan.
     * Set by the frontend after it uploads the file to S3 directly
     * (pre-signed URL flow) or via a /upload endpoint.
     * Example: "kyc/2024/user-42/aadhaar-front.jpg"
     * Maps to: kyc_document.s3_key (VARCHAR 300)
     */
    @NotBlank(message = "Document S3 key is required")
    @Size(max = 300, message = "S3 key must not exceed 300 characters")
    @Pattern(
            regexp = "^kyc/[\\w/\\-\\.]+$",
            message = "S3 key must start with 'kyc/' and contain only valid path characters"
    )
    private String s3Key;

    /**
     * S3 key for back side of document (optional — required for Aadhaar).
     * Example: "kyc/2024/user-42/aadhaar-back.jpg"
     * Maps to: a second row in kyc_document for the back side.
     */
    @Size(max = 300, message = "Back-side S3 key must not exceed 300 characters")
    private String s3KeyBack;

    // ── Address fields (optional — from Aadhaar address) ──────────

    /**
     * Address as printed on the government document (optional).
     * Used to pre-fill the user's profile address after KYC approval.
     */
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String addressOnDocument;

    /**
     * Date of birth as printed on the document (optional).
     * Format: yyyy-MM-dd
     * Cross-checked against app_user.date_of_birth in AuthService.
     */
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}$",
            message = "Date of birth must be in format yyyy-MM-dd"
    )
    private String dobOnDocument;

    /**
     * Full name as printed on the government document (optional).
     * Cross-checked against app_user.full_name in AuthService.
     */
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String nameOnDocument;

    // ── Verification consent ──────────────────────────────────────

    /**
     * User must explicitly consent to KYC data processing.
     * Mapped to a consent log in AuthService.
     */
    @NotNull(message = "KYC consent is required")
    private Boolean consentGiven;

    // ── Enum ──────────────────────────────────────────────────────

    /**
     * Mirrors the CHECK constraint in app_user.govt_id_type:
     *   CHECK (govt_id_type IN ('AADHAAR', 'VOTER_ID', 'DRIVING_LICENCE'))
     */
    public enum GovtIdType {
        AADHAAR,
        VOTER_ID,
        DRIVING_LICENCE
    }
}

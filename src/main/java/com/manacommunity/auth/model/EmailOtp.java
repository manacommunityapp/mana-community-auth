package com.manacommunity.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A short-lived one-time passcode emailed to verify an address before it is
 * allowed to submit an event registration. Codes are stored hashed; rate
 * limiting and attempt counting live in the service layer.
 */
@Entity
@Table(name = "email_otp",
        indexes = {
                @Index(name = "idx_email_otp_email", columnList = "email"),
                @Index(name = "idx_email_otp_created_at", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt hash of the numeric code — never stored in clear. */
    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    /** True once the code has been used up (verified successfully or exhausted). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean consumed = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

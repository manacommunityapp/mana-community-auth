package com.manacommunity.auth.repository;

import com.manacommunity.auth.model.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    /** Most recent still-usable code for an email (used during verify). */
    Optional<EmailOtp> findFirstByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);

    /** How many codes were issued to an email since {@code since} (rate limiting). */
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    /** Whether the email was verified within the allowed window (registration gate). */
    boolean existsByEmailAndVerifiedTrueAndVerifiedAtAfter(String email, LocalDateTime since);
}

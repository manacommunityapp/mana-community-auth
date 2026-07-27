package com.manacommunity.auth.user.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Login/session record for security monitoring. JWT auth is stateless, so this is
 * an observability trail (who logged in, from which device/IP, when) rather than a
 * server-side session store. {@code status} is a plain string to avoid enum-drift.
 */
@Entity
@Table(
    name = "user_sessions",
    indexes = {
        @Index(name = "idx_session_user", columnList = "user_id, status"),
        @Index(name = "idx_session_login", columnList = "login_at"),
        @Index(name = "idx_session_status", columnList = "status")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    public static final String ACTIVE = "ACTIVE";
    public static final String LOGGED_OUT = "LOGGED_OUT";
    public static final String EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Opaque server-generated session id (the JWT itself is never stored). */
    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 400)
    private String userAgent;

    @Column(length = 40)
    private String device;

    @Column(length = 40)
    private String browser;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;
}

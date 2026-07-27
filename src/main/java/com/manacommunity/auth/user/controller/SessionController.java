package com.manacommunity.auth.user.controller;

import com.manacommunity.auth.user.model.UserSession;
import com.manacommunity.auth.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only login/session monitoring for the admin dashboard. SUPER_ADMIN only.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SessionController {

    private final UserSessionRepository sessionRepository;

    public record SessionDto(
            Long id, Long userId, String ipAddress, String device, String browser,
            LocalDateTime loginAt, LocalDateTime lastActivityAt, LocalDateTime logoutAt, String status) {

        static SessionDto from(UserSession s) {
            return new SessionDto(s.getId(), s.getUserId(), s.getIpAddress(), s.getDevice(), s.getBrowser(),
                    s.getLoginAt(), s.getLastActivityAt(), s.getLogoutAt(), s.getStatus());
        }
    }

    public record SessionStatsResponse(long activeSessions, long loginsToday) {}

    /** GET /api/admin/sessions — most recent sessions (newest first). */
    @GetMapping("/sessions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<SessionDto>> getSessions(@RequestParam(defaultValue = "50") int limit) {
        int safe = Math.min(Math.max(limit, 1), 200);
        List<SessionDto> sessions = sessionRepository
                .findAllByOrderByLoginAtDesc(PageRequest.of(0, safe))
                .map(SessionDto::from)
                .getContent();
        return ResponseEntity.ok(sessions);
    }

    /** GET /api/admin/sessions/stats — active session count + today's logins. */
    @GetMapping("/sessions/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SessionStatsResponse> getSessionStats() {
        LocalDateTime since = LocalDate.now().atStartOfDay();
        return ResponseEntity.ok(new SessionStatsResponse(
                sessionRepository.countByStatus(UserSession.ACTIVE),
                sessionRepository.countByLoginAtAfter(since)));
    }
}

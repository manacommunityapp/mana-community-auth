package com.manacommunity.auth.user.security;

import com.manacommunity.common.security.CorrelationIdFilter;

import com.manacommunity.common.security.AuditLogService;

import com.manacommunity.auth.user.model.UserSession;
import com.manacommunity.auth.user.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tracks login sessions for security monitoring: records each login (device, browser,
 * IP, time), flags concurrent logins from multiple devices, and closes sessions on
 * logout. Never throws into the auth flow — failures are logged, not propagated.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Logger SECURITY = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final UserSessionRepository sessionRepository;
    private final AuditLogService auditLog;

    /** Open a session on successful login; flag MULTIPLE_DEVICE_LOGIN if one is already active. */
    public void startSession(Long userId, String email) {
        try {
            List<UserSession> active = sessionRepository.findByUserIdAndStatus(userId, UserSession.ACTIVE);
            if (!active.isEmpty()) {
                auditLog.record(AuditLogService.Action.MULTIPLE_DEVICE_LOGIN, userId, email);
            }
            HttpServletRequest req = currentRequest();
            String ua = req != null ? header(req, "User-Agent") : null;
            UserSession session = UserSession.builder()
                    .userId(userId)
                    .sessionId(UUID.randomUUID().toString())
                    .ipAddress(clientIp(req))
                    .userAgent(truncate(ua, 400))
                    .device(parseDevice(ua))
                    .browser(parseBrowser(ua))
                    .loginAt(LocalDateTime.now())
                    .lastActivityAt(LocalDateTime.now())
                    .status(UserSession.ACTIVE)
                    .correlationId(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID))
                    .build();
            sessionRepository.save(session);
        } catch (Exception ex) {
            SECURITY.error("Failed to record login session for userId={}: {}", userId, ex.getMessage());
        }
    }

    /** Close all active sessions for a user on logout. */
    public void endSession(Long userId) {
        try {
            List<UserSession> active = sessionRepository.findByUserIdAndStatus(userId, UserSession.ACTIVE);
            LocalDateTime now = LocalDateTime.now();
            for (UserSession s : active) {
                s.setStatus(UserSession.LOGGED_OUT);
                s.setLogoutAt(now);
            }
            sessionRepository.saveAll(active);
        } catch (Exception ex) {
            SECURITY.error("Failed to close session for userId={}: {}", userId, ex.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String header(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return (v == null || v.isBlank()) ? null : v;
    }

    private String clientIp(HttpServletRequest req) {
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private String parseBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg")) return "Edge";
        if (ua.contains("OPR") || ua.contains("Opera")) return "Opera";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        return "Other";
    }

    private String parseDevice(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS") || ua.contains("Macintosh")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        return "Other";
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

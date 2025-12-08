package com.edubas.backend.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.edubas.backend.entity.User;
import com.edubas.backend.entity.UserLogHistory;
import com.edubas.backend.repository.UserLogHistoryRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private final UserLogHistoryRepository userLogHistoryRepository;

    public AuditLogService(UserLogHistoryRepository userLogHistoryRepository) {
        this.userLogHistoryRepository = userLogHistoryRepository;
    }

    public void logUserAction(User user, String action, String status) {
        logUserAction(user.getId(), user.getUsername(), user.getEmail(), action, status, null);
    }

    public void logUserAction(User user, String action, String status, String failureReason) {
        logUserAction(user.getId(), user.getUsername(), user.getEmail(), action, status, failureReason);
    }

    public void logUserAction(String userId, String username, String email, String action, String status) {
        logUserAction(userId, username, email, action, status, null);
    }

    public void logUserAction(String userId, String username, String email, String action,
            String status, String failureReason) {
        try {
            HttpServletRequest request = getHttpServletRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";

            UserLogHistory logHistory = new UserLogHistory(
                    userId,
                    username,
                    email,
                    action,
                    status,
                    ipAddress,
                    userAgent,
                    failureReason,
                    LocalDateTime.now());

            userLogHistoryRepository.save(logHistory);
            logger.info("Audit logged - Action: {}, User: {}, Status: {}", action, username, status);
        } catch (Exception e) {
            logger.error("Failed to log audit event for user: {}, action: {}", email, action, e);
        }
    }

    public void logFailedLogin(String usernameOrEmail, String failureReason) {
        try {
            HttpServletRequest request = getHttpServletRequest();
            String ipAddress = getClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";

            UserLogHistory logHistory = new UserLogHistory(
                    null,
                    null,
                    usernameOrEmail,
                    "LOGIN",
                    "FAILURE",
                    ipAddress,
                    userAgent,
                    failureReason,
                    LocalDateTime.now());

            userLogHistoryRepository.save(logHistory);
            logger.warn("Failed login attempt for: {}, Reason: {}", usernameOrEmail, failureReason);
        } catch (Exception e) {
            logger.error("Failed to log failed login attempt for: {}", usernameOrEmail, e);
        }
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            logger.debug("Could not get HTTP request context");
            return null;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}

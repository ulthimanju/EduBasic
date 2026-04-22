package com.app.auth.user.management.service;

import com.app.auth.LogMessages;
import com.app.auth.common.config.AdminProperties;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * SpEL evaluator used by {@code @PreAuthorize} on the user-management controller.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Reject immediately if authentication is null or not authenticated.</li>
 *   <li>Reject immediately if the admin allowlist is empty (deny-by-default).</li>
 *   <li>Treat the principal as the internal user ID (set by {@code JwtAuthFilter}).</li>
 *   <li>Load the user from Neo4j; reject if not found or email is blank.</li>
 *   <li>Compare the normalized user email against the allowlist.</li>
 * </ol>
 * </p>
 *
 * <p>The bean name {@code adminAccessService} is the one referenced in
 * {@code @PreAuthorize("@adminAccessService.isAdmin(authentication)")}.</p>
 */
@Service("adminAccessService")
@RequiredArgsConstructor
@Slf4j
public class AdminAccessService {

    private final AdminProperties adminProperties;
    private final UserRepository  userRepository;

    /**
     * Returns {@code true} only when the currently authenticated principal's email
     * is present in the configured admin allowlist.
     *
     * @param auth Spring {@link Authentication} object from the SecurityContext
     * @return {@code true} if access should be granted
     */
    public boolean isAdmin(Authentication auth) {
        // 1. Reject unauthenticated callers
        if (auth == null || !auth.isAuthenticated()) {
            log.debug(LogMessages.ADMIN_CHECK_FAILED_NO_AUTH);
            return false;
        }

        // 2. Deny-by-default when allowlist is empty
        Set<String> allowedEmails = adminProperties.getAllowedEmailSet();
        if (allowedEmails.isEmpty()) {
            log.warn(LogMessages.ADMIN_ALLOWLIST_EMPTY_ACCESS_DENIED);
            return false;
        }

        // 3. Principal is the internal userId string (set by JwtAuthFilter)
        Object principal = auth.getPrincipal();
        if (!(principal instanceof String userId)) {
            log.debug(LogMessages.ADMIN_CHECK_FAILED_UNEXPECTED_PRINCIPAL, principal);
            return false;
        }

        // 4. Load user and check email
        return userRepository.findById(userId)
                .map(UserNode::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(email -> {
                    boolean granted = allowedEmails.contains(email.toLowerCase());
                    log.debug(LogMessages.ADMIN_CHECK_USER_EMAIL_GRANTED, userId, email, granted);
                    return granted;
                })
                .orElseGet(() -> {
                    log.debug(LogMessages.ADMIN_CHECK_FAILED_USER_NOT_FOUND, userId);
                    return false;
                });
    }
}

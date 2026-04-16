package com.app.auth.auth.controller;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.common.response.ApiResponse;
import com.app.auth.session.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints beyond what Spring Security handles automatically.
 *
 * <p>Only one endpoint: {@code POST /api/auth/logout}.
 * The OAuth2 initiation and callback are handled by Spring Security's
 * built-in filters at {@code /oauth2/**} and {@code /login/**}.</p>
 *
 * <p>{@code POST /api/auth/logout} is declared {@code permitAll} in
 * {@link com.app.auth.common.config.SecurityConfig} so it can be called
 * even when the session is already invalid or the cookie is missing/corrupt.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtService     jwtService;
    private final SessionService sessionService;
    private final CacheService   cacheService;
    private final CookieFactory  cookieFactory;

    /**
     * Logout the current user (best-effort, always succeeds).
     *
     * <p>Algorithm (§9.8 of design doc):
     * <ol>
     *   <li>Always clear the browser cookie (unconditional — even with no/stale token)</li>
     *   <li>If a parseable JWT is present, try to revoke the Neo4j session and evict caches</li>
     *   <li>Return 200 OK regardless — logout is idempotent</li>
     * </ol>
     *
     * <p>Best-effort means: if the JWT is malformed or the Neo4j/Redis calls fail,
     * the cookie is still cleared and the user still sees a successful logout.
     * This endpoint is public — no valid session is required to call it.</p>
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest  request,
                                                    HttpServletResponse response) {
        // Always clear the browser cookie first — unconditional
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearAuthCookie().toString());

        // Best-effort session revocation (only when we have a parseable token)
        cookieFactory.extractJwtFromRequest(request).ifPresent(jwt -> {
            try {
                String jwtId  = jwtService.extractJwtId(jwt);
                String userId = jwtService.extractUserId(jwt);

                log.info("Logout: userId={}, jwtId={}", userId, jwtId);

                // Revoke session in Neo4j
                sessionService.revokeSession(jwtId);

                // Remove from Redis immediately (don't wait for TTL to expire)
                cacheService.evictJwtCache(jwtId);
                cacheService.evictUserCache(userId);

            } catch (Exception e) {
                // Malformed token or downstream failure: swallow and continue.
                // Cookie has already been cleared above.
                log.debug("Logout best-effort: could not parse or revoke token — {}", e.getMessage());
            }
        });

        return ResponseEntity.ok(ApiResponse.<Void>ok("Logged out successfully"));
    }
}

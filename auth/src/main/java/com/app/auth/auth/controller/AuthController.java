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
     * Logout the current user.
     *
     * <p>Algorithm (§9.8 of design doc):
     * <ol>
     *   <li>Extract JWT from cookie</li>
     *   <li>Extract jwtId and userId from token</li>
     *   <li>Mark session revoked in Neo4j</li>
     *   <li>Evict JWT validity from Redis</li>
     *   <li>Evict user profile from Redis</li>
     *   <li>Set Max-Age=0 cookie to clear browser cookie</li>
     *   <li>Return 200 OK</li>
     * </ol>
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest  request,
                                                    HttpServletResponse response) {
        return cookieFactory.extractJwtFromRequest(request)
                .map(jwt -> {
                    String jwtId  = jwtService.extractJwtId(jwt);
                    String userId = jwtService.extractUserId(jwt);

                    log.info("Logout: userId={}, jwtId={}", userId, jwtId);

                    // 1. Revoke session in Neo4j
                    sessionService.revokeSession(jwtId);

                    // 2. Remove from Redis immediately (don't wait for TTL to expire)
                    cacheService.evictJwtCache(jwtId);
                    cacheService.evictUserCache(userId);

                    // 3. Clear browser cookie
                    response.addHeader(HttpHeaders.SET_COOKIE,
                            cookieFactory.clearAuthCookie().toString());

                    return ResponseEntity.ok(ApiResponse.<Void>ok("Logged out successfully"));
                })
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.<Void>ok("Already logged out")));
    }
}

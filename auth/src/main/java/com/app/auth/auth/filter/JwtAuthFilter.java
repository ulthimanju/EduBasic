package com.app.auth.auth.filter;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.common.config.SecurityConfig;
import com.app.auth.session.repository.SessionRepository;
import com.app.auth.user.dto.UserResponseDTO;
import com.app.auth.user.mapper.UserMapper;
import com.app.auth.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Validates the {@code auth_token} cookie and populates the SecurityContext.
 *
 * <p>Algorithm (§9.6 of design doc) — cache-first:
 * <ol>
 *   <li>Public route → skip filter entirely (no cookie inspection at all)</li>
 *   <li>Extract cookie — absent → pass through (unauthenticated)</li>
 *   <li>Extract jwtId from token — malformed → 401 + clear cookie</li>
 *   <li>Redis: check JWT validity cache
 *       <ul>
 *         <li>HIT "invalid" → 401 + clear cookie</li>
 *         <li>HIT "valid"   → skip to step 7</li>
 *         <li>MISS          → step 5</li>
 *       </ul>
 *   </li>
 *   <li>Validate JWT signature + expiry locally → if fails → cache invalid → 401 + clear cookie</li>
 *   <li>Neo4j: check session not revoked → if revoked → cache invalid → 401 + clear cookie; else cache valid</li>
 *   <li>Redis: lookup user profile → miss → Neo4j; if user missing → cache invalid → 401 + clear cookie</li>
 *   <li>Set SecurityContext with userId as principal</li>
 *   <li>chain.doFilter()</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService        jwtService;
    private final CacheService      cacheService;
    private final SessionRepository sessionRepository;
    private final UserService       userService;
    private final UserMapper        userMapper;
    private final CookieFactory     cookieFactory;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         chain)
            throws ServletException, IOException {

        // Step 1 — Public route: skip filter entirely, regardless of cookie state.
        // Stale or malformed cookies must never block logout, OAuth callbacks, etc.
        if (SecurityConfig.getPublicRoutes().matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        // Step 2 — No cookie → unauthenticated request; let SecurityConfig decide
        Optional<String> jwtOpt = cookieFactory.extractJwtFromRequest(request);
        if (jwtOpt.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = jwtOpt.get();

        // Step 3 — Extract jwtId (may throw on malformed token)
        String jwtId;
        try {
            jwtId = jwtService.extractJwtId(jwt);
        } catch (Exception e) {
            log.debug("Malformed JWT — cannot extract jti: {}", e.getMessage());
            sendUnauthorized(response);
            return;
        }

        // Step 4 — Redis cache hit check
        Optional<Boolean> cached = cacheService.getJwtValidity(jwtId);
        if (cached.isPresent()) {
            if (Boolean.FALSE.equals(cached.get())) {
                log.debug("JWT cached as invalid: jwtId={}", jwtId);
                sendUnauthorized(response);
                return;
            }
            // cached valid — skip Neo4j session check, jump to profile resolution
        } else {
            // Step 5 — Local JWT validation (signature + expiry)
            if (!jwtService.validateToken(jwt)) {
                log.debug("JWT local validation failed: jwtId={}", jwtId);
                cacheService.cacheJwtValidity(jwtId, false);
                sendUnauthorized(response);
                return;
            }

            // Step 6 — Neo4j session revocation check
            boolean sessionValid = sessionRepository.findBySessionId(jwtId)
                    .map(s -> !s.isRevoked())
                    .orElse(false);

            if (!sessionValid) {
                log.debug("JWT session not found or revoked: jwtId={}", jwtId);
                cacheService.cacheJwtValidity(jwtId, false);
                sendUnauthorized(response);
                return;
            }

            // Cache it as valid for subsequent requests
            cacheService.cacheJwtValidity(jwtId, true);
        }

        // Step 7 — Resolve user (cache-first)
        String userId = jwtService.extractUserId(jwt);

        Optional<UserResponseDTO> cachedUser = cacheService.getCachedUserProfile(userId);
        if (cachedUser.isEmpty()) {
            Optional<?> userNode = userService.findById(userId);
            if (userNode.isEmpty()) {
                // Backing user record is gone — token is no longer valid.
                // Mark the JWT as invalid and remove any stale user cache.
                log.warn("JWT references a user that no longer exists: userId={}, jwtId={}", userId, jwtId);
                cacheService.evictUserCache(userId);
                cacheService.cacheJwtValidity(jwtId, false);
                sendUnauthorized(response);
                return;
            }
            userNode.map(n -> userMapper.toResponseDTO((com.app.auth.user.node.UserNode) n))
                    .ifPresent(dto -> cacheService.cacheUserProfile(userId, dto));
        }

        // Step 8 — Populate SecurityContext
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Step 9 — Continue filter chain
        chain.doFilter(request, response);
    }

    /**
     * Sends a 401 JSON response and clears the auth cookie so the browser
     * promptly discards the stale/invalid token.
     */
    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearAuthCookie().toString());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"Token invalid or expired\"}");
    }
}

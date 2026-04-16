package com.app.auth.auth.filter;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
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
 *   <li>Extract cookie — absent → pass through (unauthenticated)</li>
 *   <li>Extract jwtId from token</li>
 *   <li>Redis: check JWT validity cache
 *       <ul>
 *         <li>HIT "invalid" → 401</li>
 *         <li>HIT "valid"   → skip to step 6</li>
 *         <li>MISS          → step 4</li>
 *       </ul>
 *   </li>
 *   <li>Validate JWT signature + expiry locally → if fails → cache invalid → 401</li>
 *   <li>Neo4j: check session not revoked → if revoked → cache invalid → 401; else cache valid</li>
 *   <li>Redis: lookup user profile → miss → Neo4j → cache result</li>
 *   <li>Set SecurityContext with userId as principal</li>
 *   <li>chain.doFilter()</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService       jwtService;
    private final CacheService     cacheService;
    private final SessionRepository sessionRepository;
    private final UserService      userService;
    private final UserMapper       userMapper;
    private final CookieFactory    cookieFactory;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         chain)
            throws ServletException, IOException {

        // Step 1 — No cookie → unauthenticated request; let SecurityConfig decide
        Optional<String> jwtOpt = cookieFactory.extractJwtFromRequest(request);
        if (jwtOpt.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = jwtOpt.get();

        // Step 2 — Extract jwtId (may throw on malformed token)
        String jwtId;
        try {
            jwtId = jwtService.extractJwtId(jwt);
        } catch (Exception e) {
            log.debug("Malformed JWT — cannot extract jti: {}", e.getMessage());
            sendUnauthorized(response);
            return;
        }

        // Step 3 — Redis cache hit check
        Optional<Boolean> cached = cacheService.getJwtValidity(jwtId);
        if (cached.isPresent()) {
            if (Boolean.FALSE.equals(cached.get())) {
                log.debug("JWT cached as invalid: jwtId={}", jwtId);
                sendUnauthorized(response);
                return;
            }
            // cached valid — skip Neo4j session check, jump to profile resolution
        } else {
            // Step 4 — Local JWT validation (signature + expiry)
            if (!jwtService.validateToken(jwt)) {
                log.debug("JWT local validation failed: jwtId={}", jwtId);
                cacheService.cacheJwtValidity(jwtId, false);
                sendUnauthorized(response);
                return;
            }

            // Step 5 — Neo4j session revocation check
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

        // Step 6 — Resolve user (cache-first)
        String userId = jwtService.extractUserId(jwt);

        Optional<UserResponseDTO> cachedUser = cacheService.getCachedUserProfile(userId);
        if (cachedUser.isEmpty()) {
            userService.findById(userId).ifPresent(userNode -> {
                UserResponseDTO dto = userMapper.toResponseDTO(userNode);
                cacheService.cacheUserProfile(userId, dto);
            });
        }

        // Step 7 — Populate SecurityContext
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Step 8 — Continue filter chain
        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"Token invalid or expired\"}");
    }
}

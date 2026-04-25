package com.app.auth.auth.controller;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.dto.TokenResponseDTO;
import com.app.auth.auth.service.JwtService;
import com.app.auth.cache.service.CacheService;
import com.app.auth.session.service.SessionService;
import com.app.auth.user.node.UserNode;
import com.app.auth.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtService     jwtService;
    private final UserService    userService;
    private final SessionService sessionService;
    private final CacheService   cacheService;
    private final CookieFactory  cookieFactory;

    @Value("${app.jwt.expiration-seconds}")
    private long accessTokenExpiration;

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> rtOpt = cookieFactory.extractRefreshToken(request);
        if (rtOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String refreshToken = rtOpt.get();
        if (!jwtService.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String rtId   = jwtService.extractJwtId(refreshToken);
        String userId = jwtService.extractUserId(refreshToken);

        // Verify session in Redis/Neo4j
        Optional<Boolean> cached = cacheService.getJwtValidity(rtId);
        if (cached.isPresent() && Boolean.FALSE.equals(cached.get())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // If not in cache or cached as valid, we might want to re-verify against DB if needed,
        // but for refresh rotation, we will definitely rotate now.
        
        Optional<UserNode> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            cacheService.cacheJwtValidity(rtId, false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserNode user = userOpt.get();

        // 1. Revoke old RT
        sessionService.revokeSession(rtId);
        cacheService.cacheJwtValidity(rtId, false);

        // 2. Generate new RT (Rotation)
        String newRtId = UUID.randomUUID().toString();
        String newRefreshToken = jwtService.generateRefreshToken(userId, newRtId);
        Instant rtExpiresAt = jwtService.getRefreshExpiryInstant();
        sessionService.createSession(userId, newRtId, rtExpiresAt);
        cacheService.cacheJwtValidity(newRtId, true);

        // 3. Generate new AT
        String atId = UUID.randomUUID().toString();
        java.util.List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        String accessToken = jwtService.generateToken(userId, user.getEmail(), atId, roles);
        // We don't strictly need to persist AT sessions if we rely on RT for authority, 
        // but our filter checks AT sessions too. So let's persist it.
        sessionService.createSession(userId, atId, jwtService.getExpiryInstant());
        cacheService.cacheJwtValidity(atId, true);

        // 4. Set new RT Cookie
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.buildRefreshTokenCookie(newRefreshToken).toString());

        return ResponseEntity.ok(TokenResponseDTO.builder()
                .accessToken(accessToken)
                .expiresIn(accessTokenExpiration)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> rtOpt = cookieFactory.extractRefreshToken(request);
        if (rtOpt.isPresent()) {
            String rtId = jwtService.extractJwtId(rtOpt.get());
            sessionService.revokeSession(rtId);
            cacheService.cacheJwtValidity(rtId, false);
        }
        
        // Also revoke the current Access Token if provided in header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String at = authHeader.substring(7);
            try {
                String atId = jwtService.extractJwtId(at);
                sessionService.revokeSession(atId);
                cacheService.cacheJwtValidity(atId, false);
            } catch (Exception ignored) {}
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshTokenCookie().toString());
        return ResponseEntity.ok().build();
    }
}

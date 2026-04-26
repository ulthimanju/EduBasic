package com.app.auth.auth.controller;

import com.app.auth.auth.cookie.CookieFactory;
import com.app.auth.auth.dto.TokenResponseDTO;
import com.app.auth.auth.service.JwtService;
import com.app.auth.auth.service.TokenValidator;
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
    private final TokenValidator tokenValidator;

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

        // 1. Verify session in Redis/Neo4j (Authoritative for Refresh Tokens)
        if (!tokenValidator.isRefreshTokenValid(refreshToken, rtId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<UserNode> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            tokenValidator.invalidateToken(refreshToken, rtId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserNode user = userOpt.get();

        // 2. Revoke old RT (Rotation)
        sessionService.revokeSession(rtId);
        tokenValidator.invalidateToken(refreshToken, rtId);

        // 3. Generate new RT
        String newRtId = UUID.randomUUID().toString();
        String newRefreshToken = jwtService.generateRefreshToken(userId, newRtId);
        Instant rtExpiresAt = jwtService.getRefreshExpiryInstant();
        sessionService.createSession(userId, newRtId, rtExpiresAt);

        // 4. Generate new AT
        String atId = UUID.randomUUID().toString();
        java.util.List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        String accessToken = jwtService.generateToken(userId, user.getEmail(), atId, roles);
        sessionService.createSession(userId, atId, jwtService.getExpiryInstant());

        // 5. Set new RT Cookie
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
            String rt = rtOpt.get();
            try {
                String rtId = jwtService.extractJwtId(rt);
                sessionService.revokeSession(rtId);
                tokenValidator.invalidateToken(rt, rtId);
            } catch (Exception e) {
                log.warn("Failed to revoke refresh token session during logout: {}", e.getMessage());
            }
        }

        // Also revoke the current Access Token if provided in header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String at = authHeader.substring(7);
            try {
                String atId = jwtService.extractJwtId(at);
                sessionService.revokeSession(atId);
                tokenValidator.invalidateToken(at, atId);
            } catch (Exception ignored) {}
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clearRefreshTokenCookie().toString());
        return ResponseEntity.ok().build();
    }
}

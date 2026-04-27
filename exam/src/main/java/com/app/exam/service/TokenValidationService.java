package com.app.exam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String AUTH_JWT_PREFIX = "auth:jwt:";
    private static final String JWT_INVALID     = "invalid";

    public boolean isRevoked(String jwtId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(AUTH_JWT_PREFIX + jwtId);
            return JWT_INVALID.equals(value);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to check token revocation in Redis (Redis unavailable). Rejecting token for security: {}", e.getMessage());
            return true; // Fail-closed (Reject token if we can't verify its status)
        }
    }
}

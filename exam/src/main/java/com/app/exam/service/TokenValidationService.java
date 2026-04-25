package com.app.exam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String AUTH_JWT_PREFIX = "auth:jwt:";
    private static final String JWT_INVALID     = "INVALID";

    public boolean isRevoked(String jwtId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(AUTH_JWT_PREFIX + jwtId);
            return JWT_INVALID.equals(value);
        } catch (Exception e) {
            log.warn("Failed to check token revocation in Redis: {}", e.getMessage());
            return false; // Fail-open (trust the JWT signature)
        }
    }
}

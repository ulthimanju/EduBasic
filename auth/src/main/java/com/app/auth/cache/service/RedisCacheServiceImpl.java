package com.app.auth.cache.service;

import com.app.auth.LogMessages;
import com.app.auth.common.constants.CacheConstants;
import com.app.auth.user.dto.UserResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Implements {@link CacheService} using Redis via {@link StringRedisTemplate}.
 *
 * <p>Atomic rule: each method touches exactly ONE Redis key.
 * Serialization errors are caught and logged — never propagate and crash auth.</p>
 *
 * <p>Fail-open strategy: if Redis is unreachable the method returns an empty
 * Optional or silently skips the write — the caller falls through to Neo4j.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheServiceImpl implements CacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // ── JWT validity ──────────────────────────────────────────────────────────

    @Override
    public void cacheJwtValidity(String sessionId, boolean valid) {
        cacheJwtValidity(sessionId, valid, CacheConstants.JWT_TTL_SECONDS);
    }

    @Override
    public void cacheJwtValidity(String sessionId, boolean valid, long ttlSeconds) {
        String key   = CacheConstants.AUTH_JWT_PREFIX + sessionId;
        String value = valid ? CacheConstants.JWT_VALID : CacheConstants.JWT_INVALID;
        try {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn(LogMessages.REDIS_WRITE_FAILED, key, e.getMessage());
        }
    }

    @Override
    public Optional<Boolean> getJwtValidity(String sessionId) {
        String key = CacheConstants.AUTH_JWT_PREFIX + sessionId;
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(CacheConstants.JWT_VALID.equals(value));
        } catch (Exception e) {
            log.warn(LogMessages.REDIS_READ_FAILED, key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void evictJwtCache(String sessionId) {
        String key = CacheConstants.AUTH_JWT_PREFIX + sessionId;
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn(LogMessages.REDIS_DELETE_FAILED, key, e.getMessage());
        }
    }

    // ── User profile ──────────────────────────────────────────────────────────

    @Override
    public void cacheUserProfile(String userId, UserResponseDTO user) {
        String key = CacheConstants.AUTH_USER_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(user);
            stringRedisTemplate.opsForValue().set(key, json,
                    Duration.ofSeconds(CacheConstants.USER_TTL_SECONDS));
        } catch (JsonProcessingException e) {
            log.warn(LogMessages.SERIALIZATION_FAILED, userId, e.getMessage());
        } catch (Exception e) {
            log.warn(LogMessages.REDIS_WRITE_FAILED, key, e.getMessage());
        }
    }

    @Override
    public Optional<UserResponseDTO> getCachedUserProfile(String userId) {
        String key = CacheConstants.AUTH_USER_PREFIX + userId;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, UserResponseDTO.class));
        } catch (JsonProcessingException e) {
            log.warn(LogMessages.DESERIALIZATION_FAILED, userId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn(LogMessages.REDIS_READ_FAILED, key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void evictUserCache(String userId) {
        String key = CacheConstants.AUTH_USER_PREFIX + userId;
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn(LogMessages.REDIS_DELETE_FAILED, key, e.getMessage());
        }
    }
}

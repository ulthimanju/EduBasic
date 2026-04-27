package com.nexora.courseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.constants.CacheKeys;
import com.nexora.courseservice.constants.LogMessages;
import com.nexora.courseservice.dto.response.ExamScoreCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ExamScoreCacheService {

    private static final Duration EXAM_SCORE_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ExamScoreCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveExamScore(UUID studentId, UUID examId, ExamScoreCache scoreCache) {
        try {
            String key = CacheKeys.examScore(studentId, examId);
            String value = objectMapper.writeValueAsString(scoreCache);
            redisTemplate.opsForValue().set(key, value, EXAM_SCORE_TTL);
            log.info(LogMessages.EXAM_SCORE_CACHED, studentId, examId);
        } catch (Exception e) {
            log.warn(LogMessages.CACHE_WRITE_FAILED, e.getMessage());
        }
    }

    public Optional<ExamScoreCache> getExamScore(UUID studentId, UUID examId) {
        try {
            String key = CacheKeys.examScore(studentId, examId);
            String cached = redisTemplate.opsForValue().get(key);
            if (cached == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(cached, ExamScoreCache.class));
        } catch (Exception e) {
            log.warn(LogMessages.CACHE_READ_FAILED, e.getMessage());
            return Optional.empty();
        }
    }
}

package com.nexora.courseservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.courseservice.dto.response.ExamScoreCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamScoreCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ObjectMapper objectMapper;

    private ExamScoreCacheService examScoreCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        examScoreCacheService = new ExamScoreCacheService(redisTemplate, objectMapper);
    }

    @Test
    void saveExamScore_ShouldCallRedis() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        ExamScoreCache scoreCache = new ExamScoreCache(85.0, true, LocalDateTime.now());
        String json = "{\"score\":85.0}";

        when(objectMapper.writeValueAsString(scoreCache)).thenReturn(json);

        examScoreCacheService.saveExamScore(studentId, examId, scoreCache);

        verify(valueOperations).set(anyString(), eq(json), any(Duration.class));
    }

    @Test
    void getExamScore_ShouldReturnScore_WhenPresent() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        ExamScoreCache scoreCache = new ExamScoreCache(85.0, true, LocalDateTime.now());
        String json = "{\"score\":85.0}";

        when(valueOperations.get(anyString())).thenReturn(json);
        when(objectMapper.readValue(json, ExamScoreCache.class)).thenReturn(scoreCache);

        Optional<ExamScoreCache> result = examScoreCacheService.getExamScore(studentId, examId);

        assertTrue(result.isPresent());
        assertEquals(85.0, result.get().score());
    }

    @Test
    void getExamScore_ShouldReturnEmpty_WhenMissing() {
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<ExamScoreCache> result = examScoreCacheService.getExamScore(UUID.randomUUID(), UUID.randomUUID());

        assertTrue(result.isEmpty());
    }
}

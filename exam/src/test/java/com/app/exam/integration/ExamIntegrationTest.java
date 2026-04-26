package com.app.exam.integration;

import com.app.exam.domain.*;
import com.app.exam.dto.*;
import com.app.exam.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class ExamIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @SuppressWarnings("resource")
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
            .asCompatibleSubstituteFor("apache/kafka"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionRepository questionRepository;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "INSTRUCTOR")
    void fullExamLifecycleTest() throws Exception {
        // 1. Create a question
        Question q = new Question();
        q.setType(QuestionType.MCQ_SINGLE);
        q.setTitle("Test Q");
        q.setPayload(objectMapper.createObjectNode().put("correctOptionId", "opt1"));
        q.setCreatedBy(UUID.randomUUID());
        q = questionRepository.save(q);

        // 2. Create and Publish an Exam
        CreateExamRequest createReq = new CreateExamRequest();
        createReq.setTitle("Integration Exam");
        createReq.setPassMarks(BigDecimal.valueOf(50));
        
        String examJson = mockMvc.perform(post("/api/v1/exams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        UUID examId = UUID.fromString(objectMapper.readTree(examJson).get("id").asText());

        // Add question to exam
        AddQuestionToExamRequest addQReq = new AddQuestionToExamRequest();
        addQReq.setQuestionId(q.getId());
        addQReq.setMarks(BigDecimal.valueOf(100));
        addQReq.setOrderIndex(1);
        
        mockMvc.perform(post("/api/v1/exams/{id}/questions", examId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addQReq)))
                .andExpect(status().isOk());

        // Publish
        mockMvc.perform(post("/api/v1/exams/{id}/publish", examId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "STUDENT")
    void startAttemptFlow() throws Exception {
        // This test requires a UUID principal in SecurityContext.
        // Placeholder for future implementation.
    }
}

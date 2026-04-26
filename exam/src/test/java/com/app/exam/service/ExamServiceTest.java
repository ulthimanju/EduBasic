package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.ExamQuestionMappingResponse;
import com.app.exam.dto.ExamResponse;
import com.app.exam.dto.QuestionResponse;
import com.app.exam.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamSectionRepository sectionRepository;
    @Mock
    private ExamQuestionMappingRepository mappingRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private ExamSnapshotRepository snapshotRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ExamService examService;

    private Exam exam;
    private UUID examId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        exam = new Exam();
        exam.setId(examId);
        exam.setStatus(ExamStatus.DRAFT);
    }

    @Test
    void validateForPublish_Fail_NoQuestions() {
        when(mappingRepository.countByExamId(examId)).thenReturn(0L);
        assertThrows(RuntimeException.class, () -> examService.validateForPublish(exam));
    }

    @Test
    void validateForPublish_Fail_MandatoryNoMarks() {
        when(mappingRepository.countByExamId(examId)).thenReturn(1L);
        ExamQuestionMapping mapping = new ExamQuestionMapping();
        mapping.setMandatory(true);
        mapping.setMarks(BigDecimal.ZERO);
        
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(examId))
                .thenReturn(List.of(mapping));

        assertThrows(RuntimeException.class, () -> examService.validateForPublish(exam));
    }

    @Test
    void validateForPublish_Fail_SectionNoQuestions() {
        exam.setHasSections(true);
        when(mappingRepository.countByExamId(examId)).thenReturn(1L);
        
        ExamSection section = new ExamSection();
        section.setId(UUID.randomUUID());
        section.setTitle("Empty Section");
        
        when(sectionRepository.findAllByExamIdOrderByOrderIndexAsc(examId)).thenReturn(List.of(section));
        when(mappingRepository.countBySectionId(section.getId())).thenReturn(0L);

        assertThrows(RuntimeException.class, () -> examService.validateForPublish(exam));
    }

    @Test
    void publishExam_CreatesSnapshot() {
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(mappingRepository.countByExamId(examId)).thenReturn(1L);
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(examId)).thenReturn(List.of());

        examService.publishExam(examId);

        verify(snapshotRepository).save(any(ExamSnapshot.class));
        assertEquals(ExamStatus.PUBLISHED, exam.getStatus());
    }

    @Test
    void getExam_RedactsPayloadForStudent() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("STUDENT"))).when(authentication).getAuthorities();

        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setType(QuestionType.MCQ_SINGLE);
        
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("correctOptionId", "secret");
        payload.put("otherInfo", "visible");
        question.setPayload(payload);

        ExamQuestionMapping mapping = new ExamQuestionMapping();
        mapping.setQuestion(question);
        
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(examId)).thenReturn(List.of(mapping));

        ExamResponse response = examService.getExam(examId);
        
        JsonNode redactedPayload = response.getQuestions().get(0).getQuestion().getPayload();
        assertFalse(redactedPayload.has("correctOptionId"));
        assertTrue(redactedPayload.has("otherInfo"));
    }

    @Test
    void getExam_RedactsFillBlankForStudent() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("STUDENT"))).when(authentication).getAuthorities();

        Question question = new Question();
        question.setType(QuestionType.FILL_BLANK);
        
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode blanks = objectMapper.createArrayNode();
        ObjectNode blank1 = objectMapper.createObjectNode();
        blank1.put("acceptedAnswers", "secret");
        blanks.add(blank1);
        payload.set("blanks", blanks);
        question.setPayload(payload);

        ExamQuestionMapping mapping = new ExamQuestionMapping();
        mapping.setQuestion(question);
        
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(examId)).thenReturn(List.of(mapping));

        ExamResponse response = examService.getExam(examId);
        
        JsonNode redactedPayload = response.getQuestions().get(0).getQuestion().getPayload();
        assertFalse(redactedPayload.get("blanks").get(0).has("acceptedAnswers"));
    }
}

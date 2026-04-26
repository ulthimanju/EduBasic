package com.app.exam.service;

import com.app.exam.domain.Exam;
import com.app.exam.domain.ExamQuestionMapping;
import com.app.exam.repository.ExamQuestionMappingRepository;
import com.app.exam.repository.ExamRepository;
import com.app.exam.repository.ExamSectionRepository;
import com.app.exam.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private ExamService examService;

    private Exam exam;
    private UUID examId;

    @BeforeEach
    void setUp() {
        examId = UUID.randomUUID();
        exam = new Exam();
        exam.setId(examId);
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
}

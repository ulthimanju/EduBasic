package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.*;
import com.app.exam.repository.ExamQuestionMappingRepository;
import com.app.exam.repository.ExamRepository;
import com.app.exam.repository.ExamSectionRepository;
import com.app.exam.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository sectionRepository;
    private final ExamQuestionMappingRepository mappingRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public ExamResponse createExam(CreateExamRequest request) {
        Exam exam = new Exam();
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setHasSections(request.isHasSections());
        exam.setTimeLimitMins(request.getTimeLimitMins());
        exam.setShuffleQuestions(request.isShuffleQuestions());
        exam.setShuffleOptions(request.isShuffleOptions());
        exam.setAllowBacktrack(request.isAllowBacktrack());
        exam.setMaxAttempts(request.getMaxAttempts());
        exam.setPassMarks(request.getPassMarks());
        exam.setNegativeMarking(request.isNegativeMarking());
        exam.setStatus(ExamStatus.DRAFT);

        return mapToResponse(examRepository.save(exam));
    }

    @Transactional(readOnly = true)
    public List<ExamSummaryResponse> listExams(UUID createdBy, ExamStatus status) {
        if (status != null) {
            return examRepository.findAllByCreatedByAndStatus(createdBy, status).stream()
                    .map(this::mapToSummary).collect(Collectors.toList());
        }
        return examRepository.findAllByCreatedBy(createdBy).stream()
                .map(this::mapToSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(UUID id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        return mapToFullResponse(exam);
    }

    @Transactional
    public void addSection(UUID examId, String title, String description, int orderIndex) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        if (!exam.isHasSections()) {
            throw new RuntimeException("Exam does not support sections");
        }
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT exams can be modified");
        }

        ExamSection section = new ExamSection();
        section.setExam(exam);
        section.setTitle(title);
        section.setDescription(description);
        section.setOrderIndex(orderIndex);
        sectionRepository.save(section);
    }

    @Transactional
    public void addQuestion(UUID examId, AddQuestionToExamRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT exams can be modified");
        }

        if (exam.isHasSections() && request.getSectionId() == null) {
            throw new RuntimeException("Section ID is required for sectioned exams");
        }
        if (!exam.isHasSections() && request.getSectionId() != null) {
            throw new RuntimeException("Section ID must be null for flat exams");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        ExamQuestionMapping mapping = new ExamQuestionMapping();
        mapping.setExam(exam);
        if (request.getSectionId() != null) {
            ExamSection section = sectionRepository.findById(request.getSectionId())
                    .orElseThrow(() -> new RuntimeException("Section not found"));
            mapping.setSection(section);
        }
        mapping.setQuestion(question);
        mapping.setOrderIndex(request.getOrderIndex());
        mapping.setMarks(request.getMarks());
        mapping.setNegMark(request.getNegMark());
        mapping.setMandatory(request.isMandatory());

        mappingRepository.save(mapping);
    }

    @Transactional
    public void publishExam(UUID id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));
        
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT exams can be published");
        }

        validateForPublish(exam);
        
        exam.setStatus(ExamStatus.PUBLISHED);
        examRepository.save(exam);
    }

    public void validateForPublish(Exam exam) {
        long questionCount = mappingRepository.countByExamId(exam.getId());
        if (questionCount == 0) {
            throw new RuntimeException("Exam must have at least one question");
        }

        List<ExamQuestionMapping> mappings = mappingRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
        for (ExamQuestionMapping mapping : mappings) {
            if (mapping.isMandatory() && (mapping.getMarks() == null || mapping.getMarks().compareTo(BigDecimal.ZERO) <= 0)) {
                throw new RuntimeException("All mandatory questions must have marks > 0");
            }
            if (exam.isNegativeMarking() && mapping.getNegMark() == null) {
                throw new RuntimeException("Negative marking is enabled, but some questions lack neg_mark");
            }
        }

        if (exam.isHasSections()) {
            List<ExamSection> sections = sectionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
            for (ExamSection section : sections) {
                if (mappingRepository.countBySectionId(section.getId()) == 0) {
                    throw new RuntimeException("Section '" + section.getTitle() + "' must have at least one question");
                }
            }
        }
    }

    private ExamResponse mapToResponse(Exam exam) {
        ExamResponse response = new ExamResponse();
        response.setId(exam.getId());
        response.setCreatedBy(exam.getCreatedBy());
        response.setTitle(exam.getTitle());
        response.setDescription(exam.getDescription());
        response.setHasSections(exam.isHasSections());
        response.setTimeLimitMins(exam.getTimeLimitMins());
        response.setShuffleQuestions(exam.isShuffleQuestions());
        response.setShuffleOptions(exam.isShuffleOptions());
        response.setAllowBacktrack(exam.isAllowBacktrack());
        response.setMaxAttempts(exam.getMaxAttempts());
        response.setPassMarks(exam.getPassMarks());
        response.setNegativeMarking(exam.isNegativeMarking());
        response.setStatus(exam.getStatus());
        response.setCreatedAt(exam.getCreatedAt());
        response.setUpdatedAt(exam.getUpdatedAt());
        return response;
    }

    private ExamResponse mapToFullResponse(Exam exam) {
        ExamResponse response = mapToResponse(exam);
        if (exam.isHasSections()) {
            List<ExamSection> sections = sectionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
            response.setSections(sections.stream().map(s -> {
                ExamSectionResponse sr = new ExamSectionResponse();
                sr.setId(s.getId());
                sr.setTitle(s.getTitle());
                sr.setDescription(s.getDescription());
                sr.setOrderIndex(s.getOrderIndex());
                sr.setQuestions(mappingRepository.findAllBySectionIdOrderByOrderIndexAsc(s.getId())
                        .stream().map(this::mapToMappingResponse).collect(Collectors.toList()));
                return sr;
            }).collect(Collectors.toList()));
        } else {
            response.setQuestions(mappingRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId())
                    .stream().map(this::mapToMappingResponse).collect(Collectors.toList()));
        }
        return response;
    }

    private ExamQuestionMappingResponse mapToMappingResponse(ExamQuestionMapping mapping) {
        ExamQuestionMappingResponse response = new ExamQuestionMappingResponse();
        response.setId(mapping.getId());
        response.setQuestionId(mapping.getQuestion().getId());
        response.setOrderIndex(mapping.getOrderIndex());
        response.setMarks(mapping.getMarks());
        response.setNegMark(mapping.getNegMark());
        response.setMandatory(mapping.isMandatory());
        // Optionally map question detail here if needed
        return response;
    }

    private ExamSummaryResponse mapToSummary(Exam exam) {
        ExamSummaryResponse response = new ExamSummaryResponse();
        response.setId(exam.getId());
        response.setCreatedBy(exam.getCreatedBy());
        response.setTitle(exam.getTitle());
        response.setStatus(exam.getStatus());
        return response;
    }
}

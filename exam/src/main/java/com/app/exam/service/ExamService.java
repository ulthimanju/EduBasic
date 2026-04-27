package com.app.exam.service;

import com.app.exam.client.CourseServiceClient;
import com.app.exam.domain.*;
import com.app.exam.dto.*;
import com.app.exam.repository.ExamQuestionMappingRepository;
import com.app.exam.repository.ExamRepository;
import com.app.exam.repository.ExamSectionRepository;
import com.app.exam.repository.QuestionRepository;
import com.app.exam.repository.ExamSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamSectionRepository sectionRepository;
    private final ExamQuestionMappingRepository mappingRepository;
    private final QuestionRepository questionRepository;
    private final ExamSnapshotRepository snapshotRepository;
    private final CourseServiceClient courseServiceClient;

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
    public Page<ExamSummaryResponse> listExams(UUID createdBy, ExamStatus status, Pageable pageable) {
        if (status != null) {
            return examRepository.findAllByCreatedByAndStatus(createdBy, status, pageable)
                    .map(this::mapToSummary);
        }
        return examRepository.findAllByCreatedBy(createdBy, pageable)
                .map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public Page<ExamSummaryResponse> listExamsByStatus(ExamStatus status, Pageable pageable) {
        return examRepository.findAllByStatus(status, pageable)
                .map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(UUID id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) auth.getPrincipal();
        boolean isStudent = auth.getAuthorities().contains(new SimpleGrantedAuthority("STUDENT"));
        boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        if (isStudent) {
            if (exam.getStatus() != ExamStatus.PUBLISHED) {
                throw new AccessDeniedException("Students can only access PUBLISHED exams");
            }

            // Extract token for service-to-service call
            String token = null;
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                String authHeader = attr.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || !courseServiceClient.validateStudentAccess(id, token)) {
                throw new AccessDeniedException("Student is not authorized to access this exam via any enrolled course");
            }
        } else if (!isAdmin && !exam.getCreatedBy().equals(userId)) {
            // Instructor check: must be the creator
            throw new AccessDeniedException("Instructors can only access their own exams");
        }

        return mapToFullResponse(exam);
    }

    private Exam getExamForUpdate(UUID id, UUID instructorId) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        if (!isAdmin && !exam.getCreatedBy().equals(instructorId)) {
            throw new AccessDeniedException("You do not have permission to modify this exam");
        }
        return exam;
    }

    @Transactional
    public void addSection(UUID examId, UUID instructorId, String title, String description, int orderIndex) {
        Exam exam = getExamForUpdate(examId, instructorId);
        
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
    public void addQuestion(UUID examId, UUID instructorId, AddQuestionToExamRequest request) {
        Exam exam = getExamForUpdate(examId, instructorId);
        
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
    public void publishExam(UUID id, UUID instructorId) {
        Exam exam = getExamForUpdate(id, instructorId);
        
        if (exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new RuntimeException("Only DRAFT or PUBLISHED exams can be snapshotted");
        }

        validateForPublish(exam);
        
        // 1. Create Snapshot
        ExamResponse fullDetails = mapToFullResponse(exam);
        ExamSnapshot snapshot = new ExamSnapshot();
        snapshot.setExam(exam);
        snapshot.setVersion(exam.getCurrentVersion());
        snapshot.setSnapshotData(fullDetails);
        snapshotRepository.save(snapshot);

        // 2. Update Exam Status
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
        List<ExamQuestionMapping> allMappings = mappingRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());

        if (exam.isHasSections()) {
            List<ExamSection> sections = sectionRepository.findAllByExamIdOrderByOrderIndexAsc(exam.getId());
            Map<UUID, List<ExamQuestionMapping>> sectionMap = allMappings.stream()
                    .filter(m -> m.getSection() != null)
                    .collect(Collectors.groupingBy(m -> m.getSection().getId()));

            response.setSections(sections.stream().map(s -> {
                ExamSectionResponse sr = new ExamSectionResponse();
                sr.setId(s.getId());
                sr.setTitle(s.getTitle());
                sr.setDescription(s.getDescription());
                sr.setOrderIndex(s.getOrderIndex());
                
                List<ExamQuestionMapping> sectionMappings = sectionMap.getOrDefault(s.getId(), Collections.emptyList());
                sr.setQuestions(sectionMappings.stream()
                        .map(this::mapToMappingResponse).collect(Collectors.toList()));
                return sr;
            }).collect(Collectors.toList()));
        } else {
            response.setQuestions(allMappings.stream()
                    .map(this::mapToMappingResponse).collect(Collectors.toList()));
        }
        return response;
    }

    private ExamQuestionMappingResponse mapToMappingResponse(ExamQuestionMapping mapping) {
        ExamQuestionMappingResponse response = new ExamQuestionMappingResponse();
        response.setId(mapping.getId());
        response.setQuestionId(mapping.getQuestion().getId());
        response.setQuestion(mapToQuestionResponse(mapping.getQuestion()));
        response.setOrderIndex(mapping.getOrderIndex());
        response.setMarks(mapping.getMarks());
        response.setNegMark(mapping.getNegMark());
        response.setMandatory(mapping.isMandatory());
        return response;
    }

    private QuestionResponse mapToQuestionResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setCreatedBy(question.getCreatedBy());
        response.setType(question.getType());
        response.setTitle(question.getTitle());
        response.setDescription(question.getDescription());
        
        // Redact sensitive payload fields for students
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("STUDENT"));
        
        if (isStudent && question.getPayload() != null) {
            ObjectNode redactedPayload = question.getPayload().deepCopy();
            redactedPayload.remove("correctOptionId");
            redactedPayload.remove("correctOptionIds");
            redactedPayload.remove("correctAnswer");
            redactedPayload.remove("testCases");
            redactedPayload.remove("correctPairs"); // For MATCH questions
            redactedPayload.remove("correctOrder"); // For SEQUENCE questions
            
            // For FILL_BLANK: remove acceptedAnswers from each blank
            if (redactedPayload.has("blanks") && redactedPayload.get("blanks").isArray()) {
                redactedPayload.get("blanks").forEach(blank -> {
                    if (blank instanceof ObjectNode) {
                        ((ObjectNode) blank).remove("acceptedAnswers");
                    }
                });
            }
            
            response.setPayload(redactedPayload);
        } else {
            response.setPayload(question.getPayload());
        }

        response.setDifficulty(question.getDifficulty());
        response.setTags(question.getTags());
        response.setDefaultMarks(question.getDefaultMarks());
        response.setDefaultNegMark(question.getDefaultNegMark());
        response.setPublic(question.isPublic());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());
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

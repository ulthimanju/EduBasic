package com.app.exam.service;

import com.app.exam.domain.Difficulty;
import com.app.exam.domain.Question;
import com.app.exam.domain.QuestionType;
import com.app.exam.dto.CreateQuestionRequest;
import com.app.exam.dto.QuestionResponse;
import com.app.exam.dto.QuestionSummaryResponse;
import com.app.exam.repository.QuestionRepository;
import com.app.exam.repository.QuestionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionPayloadValidator payloadValidator;

    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        payloadValidator.validate(request.getType(), request.getPayload());
        
        Question question = new Question();
        question.setCreatedBy(getCurrentUserId());
        question.setType(request.getType());
        question.setTitle(request.getTitle());
        question.setDescription(request.getDescription());
        question.setPayload(request.getPayload());
        question.setDifficulty(request.getDifficulty());
        question.setTags(normalizeTags(request.getTags()));
        question.setDefaultMarks(request.getDefaultMarks());
        question.setDefaultNegMark(request.getDefaultNegMark());
        question.setPublic(request.isPublic());

        return mapToResponse(questionRepository.save(question));
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummaryResponse> listQuestions(QuestionType type, Difficulty difficulty, UUID createdBy, String tag, Pageable pageable) {
        UUID currentUserId = getCurrentUserId();
        boolean isAdmin = isAdmin();
        
        // Instructors can only list their own questions, ignoring caller-supplied createdBy
        UUID effectiveCreatedBy = isAdmin ? createdBy : currentUserId;

        Specification<Question> spec = Specification.allOf(
                QuestionSpecification.hasType(type),
                QuestionSpecification.hasDifficulty(difficulty),
                QuestionSpecification.hasCreatedBy(effectiveCreatedBy),
                QuestionSpecification.hasTag(tag)
        );
        
        return questionRepository.findAll(spec, pageable).map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!isAdmin() && !question.getCreatedBy().equals(getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to view this question");
        }

        return mapToResponse(question);
    }

    @Transactional
    public QuestionResponse updateQuestion(UUID id, CreateQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        if (!isAdmin() && !question.getCreatedBy().equals(getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to update this question");
        }

        payloadValidator.validate(request.getType(), request.getPayload());

        question.setType(request.getType());
        question.setTitle(request.getTitle());
        question.setDescription(request.getDescription());
        question.setPayload(request.getPayload());
        question.setDifficulty(request.getDifficulty());
        question.setTags(normalizeTags(request.getTags()));
        question.setDefaultMarks(request.getDefaultMarks());
        question.setDefaultNegMark(request.getDefaultNegMark());
        question.setPublic(request.isPublic());

        return mapToResponse(questionRepository.save(question));
    }

    @Transactional
    public void deleteQuestion(UUID id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!isAdmin() && !question.getCreatedBy().equals(getCurrentUserId())) {
            throw new AccessDeniedException("You do not have permission to delete this question");
        }

        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public List<String> listTags() {
        return questionRepository.findAllDistinctTags();
    }

    @Transactional
    public void bulkImport(List<CreateQuestionRequest> requests) {
        for (CreateQuestionRequest request : requests) {
            createQuestion(request);
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return (UUID) auth.getPrincipal();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) return null;
        return tags.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());
    }

    private QuestionResponse mapToResponse(Question question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setCreatedBy(question.getCreatedBy());
        response.setType(question.getType());
        response.setTitle(question.getTitle());
        response.setDescription(question.getDescription());
        response.setPayload(question.getPayload());
        response.setDifficulty(question.getDifficulty());
        response.setTags(question.getTags());
        response.setDefaultMarks(question.getDefaultMarks());
        response.setDefaultNegMark(question.getDefaultNegMark());
        response.setPublic(question.isPublic());
        response.setCreatedAt(question.getCreatedAt());
        response.setUpdatedAt(question.getUpdatedAt());
        return response;
    }

    private QuestionSummaryResponse mapToSummary(Question question) {
        QuestionSummaryResponse response = new QuestionSummaryResponse();
        response.setId(question.getId());
        response.setCreatedBy(question.getCreatedBy());
        response.setType(question.getType());
        response.setTitle(question.getTitle());
        response.setDifficulty(question.getDifficulty());
        response.setTags(question.getTags());
        return response;
    }
}

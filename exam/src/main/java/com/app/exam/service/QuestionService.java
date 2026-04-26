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
        Specification<Question> spec = Specification.where(QuestionSpecification.hasType(type))
                .and(QuestionSpecification.hasDifficulty(difficulty))
                .and(QuestionSpecification.hasCreatedBy(createdBy))
                .and(QuestionSpecification.hasTag(tag));
        
        return questionRepository.findAll(spec, pageable).map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID id) {
        return questionRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Question not found"));
    }

    @Transactional
    public QuestionResponse updateQuestion(UUID id, CreateQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
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
        questionRepository.deleteById(id);
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

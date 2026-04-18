package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.*;
import com.app.exam.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final ExamSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final ResultRepository resultRepository;
    private final AdaptiveEngine adaptiveEngine;
    private final ScoreEngine scoreEngine;
    private final LevelClassifier levelClassifier;

    private static final int MAX_QUESTIONS = 20;

    public ExamSession startExam(UUID userId, StartExamRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        ExamSession session = ExamSession.builder()
                .userId(userId)
                .course(course)
                .currentIndex(0)
                .currentDifficulty("MEDIUM")
                .streak(0)
                .status(ExamSession.Status.ACTIVE)
                .build();

        return sessionRepository.save(session);
    }

    public QuestionResponse getCurrentQuestion(UUID sessionId) {
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != ExamSession.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam session is not active");
        }

        List<Question> questions = questionRepository.findRandomByCourseIdAndDifficulty(
                session.getCourse().getId(), session.getCurrentDifficulty(), 1);

        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No questions available");
        }

        Question question = questions.get(0);

        return QuestionResponse.builder()
                .sessionId(session.getId())
                .questionId(question.getId())
                .question(question.getQuestion())
                .options(question.getOptions())
                .index(session.getCurrentIndex() + 1)
                .difficulty(session.getCurrentDifficulty())
                .timeLimit(60)
                .build();
    }

    @Transactional
    public AnswerResponse submitAnswer(UUID sessionId, AnswerRequest request) {
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != ExamSession.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam session is not active");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(request.getSelectedOption());

        // Update streak
        if (isCorrect) {
            session.setStreak(Math.max(1, session.getStreak() + 1));
        } else {
            session.setStreak(Math.min(-1, session.getStreak() - 1));
        }

        // Save answer
        UserAnswer answer = UserAnswer.builder()
                .session(session)
                .questionId(question.getId())
                .selectedOption(request.getSelectedOption())
                .isCorrect(isCorrect)
                .timeTaken(request.getTimeTaken())
                .build();
        userAnswerRepository.save(answer);

        // Update session
        session.setCurrentIndex(session.getCurrentIndex() + 1);
        
        boolean sessionComplete = session.getCurrentIndex() >= MAX_QUESTIONS;
        
        if (sessionComplete) {
            session.setStatus(ExamSession.Status.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            calculateAndSaveResult(session);
        } else {
            String nextDifficulty = adaptiveEngine.getNextDifficulty(session, isCorrect, false);
            session.setCurrentDifficulty(nextDifficulty);
        }

        sessionRepository.save(session);

        return AnswerResponse.builder()
                .correct(isCorrect)
                .correctAnswer(question.getCorrectAnswer())
                .explanation(question.getExplanation())
                .sessionComplete(sessionComplete)
                .build();
    }

    private void calculateAndSaveResult(ExamSession session) {
        List<UserAnswer> answers = userAnswerRepository.findBySessionId(session.getId());
        
        float rawScore = 0;
        int maxPossibleRaw = 0;
        
        for (UserAnswer ans : answers) {
            // This logic is simplified; we'd need the question difficulty at the time of answer
            // For now, assume we can fetch it from Question repository or it was saved in UserAnswer
            // I'll assume we can fetch it for now.
            Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);
            if (q != null) {
                rawScore += scoreEngine.calculatePoints(q.getDifficulty(), ans.getIsCorrect(), ans.getTimeTaken(), 60);
                maxPossibleRaw += 4; // Assuming max 4 points per question
            }
        }
        
        float normalizedScore = (rawScore / maxPossibleRaw) * 100;
        String level = levelClassifier.classify(normalizedScore);
        
        ExamResult result = ExamResult.builder()
                .session(session)
                .userId(session.getUserId())
                .course(session.getCourse())
                .level(level)
                .rawScore(rawScore)
                .normalizedScore(normalizedScore)
                .createdAt(LocalDateTime.now())
                .build();
        
        resultRepository.save(result);
    }

    public ExamResultResponse getResult(UUID sessionId) {
        ExamResult result = resultRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));

        return ExamResultResponse.builder()
                .sessionId(sessionId)
                .level(result.getLevel())
                .rawScore(result.getRawScore())
                .normalizedScore(result.getNormalizedScore())
                .topicsStrong(result.getTopicsStrong())
                .topicsWeak(result.getTopicsWeak())
                .difficultyBreakdown(result.getDifficultyBreakdown())
                .build();
    }
}

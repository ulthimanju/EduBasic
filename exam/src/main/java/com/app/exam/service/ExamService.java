package com.app.exam.service;

import com.app.exam.domain.*;
import com.app.exam.dto.*;
import com.app.exam.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamService {
    private final GeminiService geminiService;
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
                .violationCount(0)
                .build();

        return sessionRepository.save(session);
    }

    @Transactional
    public QuestionResponse getCurrentQuestion(UUID sessionId) {
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != ExamSession.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam session is not active. Current status: " + session.getStatus());
        }

        // Update activity timestamp
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        String difficulty = session.getCurrentDifficulty();
        
        // 1. Try to find an UNUSED question in the DB first (could be GEMINI or FALLBACK)
        List<Question> questions = questionRepository.findRandomByCourseIdAndDifficultyAndNotUsedInSession(
                session.getCourse().getId(), difficulty, session.getId(), 1);

        // 2. If none, trigger REAL-TIME generation from Gemini
        if (questions.isEmpty()) {
            log.info("No unused questions in DB for {} - {}. Triggering Gemini AI generation.", 
                    session.getCourse().getName(), difficulty);
            
            List<Question> generated = geminiService.generateQuestions(
                    session.getCourse().getName(), 
                    List.of(), // Could add topics from session history here
                    difficulty, 
                    3 // Generate a small batch
            );

            if (!generated.isEmpty()) {
                // Attach course and source=GEMINI, then save
                generated.forEach(q -> {
                    q.setId(UUID.randomUUID());
                    q.setCourse(session.getCourse());
                    q.setSource("GEMINI");
                });
                questionRepository.saveAll(generated);
                
                // Pick the first one from the new batch
                questions = List.of(generated.get(0));
            }
        }

        // 3. Fallback Ladder (If AI failed or DB is empty)
        if (questions.isEmpty() && !"MEDIUM".equalsIgnoreCase(difficulty)) {
            log.info("AI/DB empty for {}. Falling back to MEDIUM.", difficulty);
            questions = questionRepository.findRandomByCourseIdAndDifficultyAndNotUsedInSession(
                    session.getCourse().getId(), "MEDIUM", session.getId(), 1);
        }

        if (questions.isEmpty()) {
            log.warn("Emergency fallback: Picking any available unused question for course {}.", session.getCourse().getId());
            questions = questionRepository.findRandomByCourseIdAndNotUsedInSession(
                    session.getCourse().getId(), session.getId(), 1);
        }

        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No questions available for this course. Please contact support.");
        }

        Question question = questions.get(0);

        return QuestionResponse.builder()
                .sessionId(session.getId())
                .questionId(question.getId())
                .question(question.getQuestion())
                .options(question.getOptions())
                .index(session.getCurrentIndex() + 1)
                .difficulty(question.getDifficulty())
                .timeLimit(60)
                .warningMessage(session.getWarningMessage())
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
        session.setLastActivityAt(LocalDateTime.now());
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
                .warningMessage(session.getWarningMessage())
                .build();
    }

    @Transactional
    public ExamSession reportViolation(UUID sessionId, ViolationRequest request) {
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != ExamSession.Status.ACTIVE) {
            return session;
        }

        session.setViolationCount(session.getViolationCount() + 1);
        session.setLastActivityAt(LocalDateTime.now());
        
        log.warn("Integrity violation reported for session {}: {}. Count: {}", 
                sessionId, request.getReason(), session.getViolationCount());

        if (session.getViolationCount() >= 2) {
            return terminateSession(session, "Multiple integrity violations: " + request.getReason());
        }

        session.setWarningMessage("Warning: Integrity violation detected. Multiple violations will result in exam termination. Reason: " + request.getReason());
        return sessionRepository.save(session);
    }

    @Transactional
    public ExamSession terminateSession(UUID sessionId, String reason) {
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        return terminateSession(session, reason);
    }

    private ExamSession terminateSession(ExamSession session, String reason) {
        if (session.getStatus() != ExamSession.Status.ACTIVE) {
            return session;
        }

        log.info("Terminating session {} due to: {}", session.getId(), reason);
        session.setStatus(ExamSession.Status.TERMINATED);
        session.setTerminationReason(reason);
        session.setCompletedAt(LocalDateTime.now());
        session.setLastActivityAt(LocalDateTime.now());
        
        calculateAndSaveResult(session);
        
        return sessionRepository.save(session);
    }

    private void calculateAndSaveResult(ExamSession session) {
        List<UserAnswer> answers = userAnswerRepository.findBySessionId(session.getId());
        
        float rawScore = 0;
        int maxPossibleRaw = 0;
        
        for (UserAnswer ans : answers) {
            Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);
            if (q != null) {
                rawScore += scoreEngine.calculatePoints(q.getDifficulty(), ans.getIsCorrect(), ans.getTimeTaken(), 60);
                maxPossibleRaw += 4;
            }
        }

        // Handle case with no answers
        if (maxPossibleRaw == 0) maxPossibleRaw = 1;
        
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
        ExamSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        
        if (session.getStatus() == ExamSession.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam is still in progress. Complete the exam to see results.");
        }

        ExamResult result = resultRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found for this session. It might have been abandoned or errored."));

        return ExamResultResponse.builder()
                .sessionId(sessionId)
                .level(result.getLevel())
                .rawScore(result.getRawScore())
                .normalizedScore(result.getNormalizedScore())
                .topicsStrong(result.getTopicsStrong())
                .topicsWeak(result.getTopicsWeak())
                .difficultyBreakdown(result.getDifficultyBreakdown())
                .status(session.getStatus().name())
                .terminationReason(session.getTerminationReason())
                .warningMessage(session.getWarningMessage())
                .violationCount(session.getViolationCount())
                .build();
    }
}

package com.app.exam.service;

import com.app.exam.domain.ExamSession;
import org.springframework.stereotype.Service;

@Service
public class AdaptiveEngine {

    public String getNextDifficulty(ExamSession session, boolean lastCorrect, boolean timeout) {
        int currentIndex = session.getCurrentIndex();
        String currentDifficulty = session.getCurrentDifficulty();
        int streak = session.getStreak();

        // Phase 1 (Q1–Q5): Fixed MEDIUM — calibration
        if (currentIndex < 5) {
            return "MEDIUM";
        }

        // Phase 2 (Q6–Q20): Adaptive rules
        if (timeout || !lastCorrect) {
            // Drop difficulty down if 2 consecutive wrong
            if (streak <= -2) {
                return downgrade(currentDifficulty);
            }
        } else {
            // Bump difficulty up if 3 consecutive correct
            if (streak >= 3) {
                return upgrade(currentDifficulty);
            }
        }

        return currentDifficulty;
    }

    private String upgrade(String current) {
        return switch (current) {
            case "EASY" -> "MEDIUM";
            case "MEDIUM" -> "HARD";
            case "HARD", "EXPERT" -> "EXPERT";
            default -> "MEDIUM";
        };
    }

    private String downgrade(String current) {
        return switch (current) {
            case "EXPERT" -> "HARD";
            case "HARD" -> "MEDIUM";
            case "MEDIUM", "EASY" -> "EASY";
            default -> "MEDIUM";
        };
    }
}

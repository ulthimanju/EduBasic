package com.app.exam.service;

import org.springframework.stereotype.Service;

@Service
public class ScoreEngine {

    public float calculatePoints(String difficulty, boolean correct, int timeTaken, int timeLimit) {
        if (!correct) return 0;

        float points = switch (difficulty) {
            case "EASY" -> 1.0f;
            case "MEDIUM" -> 2.0f;
            case "HARD" -> 3.0f;
            case "EXPERT" -> 4.0f;
            default -> 1.0f;
        };

        // Speed bonus: answered in < 30% of time limit → +0.5 pt
        if (timeTaken < timeLimit * 0.3) {
            points += 0.5f;
        }

        return points;
    }

    public float calculateStreakBonus(int streak) {
        // Streak bonus: 5+ correct in a row → +2 pt flat
        return streak >= 5 ? 2.0f : 0.0f;
    }
}

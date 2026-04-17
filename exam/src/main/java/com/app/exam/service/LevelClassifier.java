package com.app.exam.service;

import org.springframework.stereotype.Service;

@Service
public class LevelClassifier {

    public String classify(float normalizedScore) {
        if (normalizedScore < 40) return "Basic";
        if (normalizedScore < 60) return "Intermediate";
        if (normalizedScore < 80) return "Advanced";
        return "Expert";
    }
}

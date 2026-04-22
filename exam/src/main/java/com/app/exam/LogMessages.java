package com.app.exam;

/**
 * Centrally managed log messages for the Exam service.
 */
public final class LogMessages {
    private LogMessages() {}

    public static final String FOUND_STALE_SESSIONS_ABANDONED = "Found {} stale sessions to mark as ABANDONED";
    public static final String JWT_VALIDATION_FAILED = "JWT validation failed: {}";
    public static final String GENERATING_QUESTIONS_GEMINI = "Generating {} questions for {} using Gemini at {}";
    public static final String ERROR_CALLING_GEMINI_API = "Error calling Gemini API: {}";
    public static final String NO_UNUSED_QUESTIONS_IN_DB = "No unused questions in DB for {} - {}. Triggering Gemini AI generation.";
    public static final String AI_DB_EMPTY_FALLBACK_MEDIUM = "AI/DB empty for {}. Falling back to MEDIUM.";
    public static final String EMERGENCY_FALLBACK_PICKING_QUESTION = "Emergency fallback: Picking any available unused question for course {}.";
    public static final String INTEGRITY_VIOLATION_REPORTED = "Integrity violation reported for session {}: {}. Count: {}";
    public static final String TERMINATING_SESSION_REASON = "Terminating session {} due to: {}";
}

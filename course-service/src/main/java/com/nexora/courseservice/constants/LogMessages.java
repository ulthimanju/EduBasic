package com.nexora.courseservice.constants;

public final class LogMessages {
    private LogMessages() {}
    public static final String COURSE_CREATED          = "Course created: {}";
    public static final String COURSE_PUBLISHED        = "Course published: {}";
    public static final String STUDENT_ENROLLED        = "Student {} enrolled in course {}";
    public static final String PROGRESS_UPDATED        = "Progress updated for student {} lesson {}";
    public static final String EXAM_COMPLETED_RECEIVED = "Exam completed event received for student {} exam {}";
    public static final String COMPLETION_CHECK_TRIGGERED = "Completion check triggered for student {} course {}";
    public static final String COURSE_COMPLETED        = "Course completed for student {} course {}";
    public static final String EXAM_SERVICE_UNREACHABLE  = "Exam service unreachable for examId: {}";
}

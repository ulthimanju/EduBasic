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
    public static final String CACHE_READ_FAILED   = "Cache read failed, falling through to DB: {}";
    public static final String CACHE_EVICT_FAILED  = "Cache eviction failed for key: {}";
    public static final String CACHE_WRITE_FAILED  = "Cache write failed: {}";
    public static final String ENROLLMENT_CREATED       = "Student {} enrolled in course {}";
    public static final String ENROLLMENT_DROPPED_LOG   = "Student {} dropped course {}";
    public static final String PROGRESS_UPDATED_LOG     = "Progress updated: student={} lesson={} percent={}";
    public static final String COMPLETION_CHECK_PASS    = "All completion rules passed for student={} course={}";
    public static final String COMPLETION_CHECK_FAIL    = "Completion check failed for student={} course={} reason={}";
}

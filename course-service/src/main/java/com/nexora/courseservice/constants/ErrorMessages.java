package com.nexora.courseservice.constants;

public final class ErrorMessages {
    private ErrorMessages() {}
    public static final String COURSE_NOT_FOUND        = "Course not found";
    public static final String MODULE_NOT_FOUND        = "Module not found";
    public static final String LESSON_NOT_FOUND        = "Lesson not found";
    public static final String ENROLLMENT_NOT_FOUND    = "Enrollment not found";
    public static final String ALREADY_ENROLLED        = "Already enrolled in this course";
    public static final String COURSE_NOT_PUBLISHED    = "Course is not published";
    public static final String UNAUTHORIZED_ACCESS     = "You do not have access to this resource";
    public static final String INVALID_STATUS_TRANSITION = "Invalid status transition";
    public static final String EXAM_NOT_FOUND            = "Exam not found in exam-service";
    public static final String EXAM_SERVICE_UNAVAILABLE  = "Exam service is currently unavailable";
    public static final String EXAM_NOT_PUBLISHED        = "Linked exam is not published";
    public static final String NOT_ENROLLED             = "Student is not enrolled in this course";
    public static final String ENROLLMENT_DROPPED       = "Cannot access content — enrollment is dropped";
    public static final String PROGRESS_CANNOT_REGRESS  = "Progress cannot go backwards";
}

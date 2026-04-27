package com.nexora.courseservice.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class CourseServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public CourseServiceException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}

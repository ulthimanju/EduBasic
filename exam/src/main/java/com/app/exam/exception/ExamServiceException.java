package com.app.exam.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExamServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public ExamServiceException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}

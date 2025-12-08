package com.edubas.backend.dto;

public class SubmitSolutionResponse {
    private String solutionId;
    private String message;
    private boolean success;

    public SubmitSolutionResponse() {
    }

    public SubmitSolutionResponse(String solutionId, String message, boolean success) {
        this.solutionId = solutionId;
        this.message = message;
        this.success = success;
    }

    // Getters and Setters
    public String getSolutionId() {
        return solutionId;
    }

    public void setSolutionId(String solutionId) {
        this.solutionId = solutionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}

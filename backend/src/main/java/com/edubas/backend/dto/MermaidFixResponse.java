package com.edubas.backend.dto;

public class MermaidFixResponse {
    private boolean success;
    private String fixedCode;
    private String message;

    public MermaidFixResponse() {
    }

    public MermaidFixResponse(boolean success, String fixedCode, String message) {
        this.success = success;
        this.fixedCode = fixedCode;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFixedCode() {
        return fixedCode;
    }

    public void setFixedCode(String fixedCode) {
        this.fixedCode = fixedCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

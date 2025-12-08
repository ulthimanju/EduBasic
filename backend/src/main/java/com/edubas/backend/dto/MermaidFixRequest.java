package com.edubas.backend.dto;

public class MermaidFixRequest {
    private String brokenCode;
    private String lessonTitle;
    private String lessonContext;

    public MermaidFixRequest() {
    }

    public MermaidFixRequest(String brokenCode, String lessonTitle, String lessonContext) {
        this.brokenCode = brokenCode;
        this.lessonTitle = lessonTitle;
        this.lessonContext = lessonContext;
    }

    public String getBrokenCode() {
        return brokenCode;
    }

    public void setBrokenCode(String brokenCode) {
        this.brokenCode = brokenCode;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public String getLessonContext() {
        return lessonContext;
    }

    public void setLessonContext(String lessonContext) {
        this.lessonContext = lessonContext;
    }
}

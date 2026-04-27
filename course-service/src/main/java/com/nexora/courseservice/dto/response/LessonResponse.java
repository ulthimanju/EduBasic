package com.nexora.courseservice.dto.response;

import com.nexora.courseservice.entity.ContentType;
import lombok.Data;

import java.util.UUID;

@Data
public class LessonResponse {
    private UUID id;
    private String title;
    private ContentType contentType;
    private String contentBody;
    private String contentUrl;
    private Integer durationMinutes;
    private int orderIndex;
    private boolean isPreview;
}

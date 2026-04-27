package com.nexora.courseservice.dto.request;

import com.nexora.courseservice.entity.ContentType;
import lombok.Data;

@Data
public class UpdateLessonRequest {
    private String title;
    private ContentType contentType;
    private String contentBody;
    private String contentUrl;
    private Integer durationMinutes;
    private Integer orderIndex;
    private Boolean isPreview;
}

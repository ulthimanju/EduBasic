package com.nexora.courseservice.dto.request;

import com.nexora.courseservice.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLessonRequest {
    @NotBlank(message = "Lesson title is required")
    private String title;
    @NotNull(message = "Content type is required")
    private ContentType contentType;
    private String contentBody;
    private String contentUrl;
    private Integer durationMinutes;
    @NotNull(message = "Order index is required")
    private Integer orderIndex;
    private boolean isPreview = false;
}

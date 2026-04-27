package com.nexora.courseservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lessons")
@Getter
@Setter
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "content_body", columnDefinition = "TEXT")
    private String contentBody;

    @Column(name = "content_url", columnDefinition = "TEXT")
    private String contentUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "is_preview", nullable = false)
    private boolean isPreview = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}

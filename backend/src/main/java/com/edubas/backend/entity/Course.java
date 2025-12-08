package com.edubas.backend.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("Course")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    private String id;

    private String courseId;
    private String title;
    private String description;

    private String uploadedBy;
    private String uploadedByUserId;
    private LocalDateTime uploadedOn;
    private String ipAddress;

    @Relationship(type = "HAS_LEVEL", direction = Relationship.Direction.OUTGOING)
    private List<Level> levels;

    public Course(String courseId, String title, String description, List<Level> levels,
            String uploadedBy, String uploadedByUserId, LocalDateTime uploadedOn, String ipAddress) {
        this.id = UUID.randomUUID().toString();
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.levels = levels;
        this.uploadedBy = uploadedBy;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedOn = uploadedOn;
        this.ipAddress = ipAddress;
    }
}

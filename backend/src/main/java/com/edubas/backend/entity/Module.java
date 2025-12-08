package com.edubas.backend.entity;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Node("Module")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Module {

    @Id
    private String id;

    private String moduleId;
    private String moduleTitle;
    private String description;
    private int estimatedTimeMinutes;

    @Relationship(type = "HAS_LESSON", direction = Relationship.Direction.OUTGOING)
    private List<Lesson> lessons;

    public Module(String moduleId, String moduleTitle, String description, int estimatedTimeMinutes,
            List<Lesson> lessons) {
        this.id = UUID.randomUUID().toString();
        this.moduleId = moduleId;
        this.moduleTitle = moduleTitle;
        this.description = description;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.lessons = lessons;
    }
}

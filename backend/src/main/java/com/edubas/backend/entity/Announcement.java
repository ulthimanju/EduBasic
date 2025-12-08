package com.edubas.backend.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("Announcement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    private String title;
    private String description;
    private String type; // "announcement" or "update"
    private Long createdAt;
    private Long updatedAt;

    @Relationship(type = "ANNOUNCED_BY", direction = Relationship.Direction.OUTGOING)
    private User createdBy;

    public Announcement(String title, String description, String type, User createdBy) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}

package com.edubas.backend.entity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("Level")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Level {

    @Id
    private String id;

    private String levelId;
    private String levelName;
    private String summary;

    @Relationship(type = "HAS_MODULE", direction = Relationship.Direction.OUTGOING)
    private List<Module> modules;

    public Level(String levelId, String levelName, String summary, List<Module> modules) {
        this.id = UUID.randomUUID().toString();
        this.levelId = levelId;
        this.levelName = levelName;
        this.summary = summary;
        this.modules = modules;
    }
}

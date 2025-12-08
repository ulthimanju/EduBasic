package com.edubas.backend.entity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("Lesson")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    private String id;

    private String lessonId;
    private String title;
    private List<String> objectives;
    private String theoryMarkdown;
    private String examplesJson;
    private String visualizationJson;
    private String quizJson;

    public Lesson(String lessonId, String title, List<String> objectives, String theoryMarkdown,
            String examplesJson, String visualizationJson, String quizJson) {
        this.id = UUID.randomUUID().toString();
        this.lessonId = lessonId;
        this.title = title;
        this.objectives = objectives;
        this.theoryMarkdown = theoryMarkdown;
        this.examplesJson = examplesJson;
        this.visualizationJson = visualizationJson;
        this.quizJson = quizJson;
    }
}

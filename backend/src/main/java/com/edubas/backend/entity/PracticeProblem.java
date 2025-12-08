package com.edubas.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Node("PracticeProblem")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProblem {

    @Id
    private String id;

    private String title;
    private String statement;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private String hintsJson; // JSON string of hints array
    private String testCasesJson; // JSON string of test cases array
    private LocalDateTime createdAt;

    @Relationship(type = "GENERATED_BY", direction = Relationship.Direction.OUTGOING)
    private User generatedBy;

    @Relationship(type = "FOR_LESSON", direction = Relationship.Direction.OUTGOING)
    private Lesson forLesson;

    public PracticeProblem(String title, String statement, String inputFormat, String outputFormat,
            String constraints, String hintsJson, String testCasesJson, User generatedBy, Lesson forLesson) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.statement = statement;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.constraints = constraints;
        this.hintsJson = hintsJson;
        this.testCasesJson = testCasesJson;
        this.createdAt = LocalDateTime.now();
        this.generatedBy = generatedBy;
        this.forLesson = forLesson;
    }
}

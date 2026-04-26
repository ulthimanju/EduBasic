package com.app.exam.repository;

import com.app.exam.domain.Difficulty;
import com.app.exam.domain.Question;
import com.app.exam.domain.QuestionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class QuestionSpecification {
    public static Specification<Question> hasType(QuestionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Question> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> difficulty == null ? null : cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Question> hasCreatedBy(UUID createdBy) {
        return (root, query, cb) -> createdBy == null ? null : cb.equal(root.get("createdBy"), createdBy);
    }

    public static Specification<Question> hasTag(String tag) {
        return (root, query, cb) -> tag == null ? null : cb.isMember(tag, root.get("tags"));
    }
}

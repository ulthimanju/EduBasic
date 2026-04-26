package com.app.exam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "question_bank")
@Getter
@Setter
@SQLDelete(sql = "UPDATE question_bank SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class QuestionBank extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String subject;

    private String topic;

    @Column(name = "owner_id")
    private String ownerId;
}

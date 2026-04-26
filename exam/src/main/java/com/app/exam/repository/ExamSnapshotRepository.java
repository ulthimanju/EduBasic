package com.app.exam.repository;

import com.app.exam.domain.ExamSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamSnapshotRepository extends JpaRepository<ExamSnapshot, UUID> {
    Optional<ExamSnapshot> findByExamIdAndVersion(UUID examId, Integer version);
}

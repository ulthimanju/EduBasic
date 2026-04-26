package com.app.sandbox.repository;

import com.app.sandbox.domain.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, UUID> {
}

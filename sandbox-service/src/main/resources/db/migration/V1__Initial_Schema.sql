CREATE TABLE code_submissions (
    id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    question_id UUID NOT NULL,
    language VARCHAR(255),
    source_code TEXT,
    status VARCHAR(50),
    overall_status VARCHAR(50),
    test_case_results JSONB,
    execution_time_ms INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    result_published BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_submission_attempt_id ON code_submissions(attempt_id);
CREATE INDEX idx_submission_status ON code_submissions(status);

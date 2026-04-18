ALTER TABLE exam_sessions ADD COLUMN violation_count INT DEFAULT 0;
ALTER TABLE exam_sessions ADD COLUMN termination_reason VARCHAR(255);

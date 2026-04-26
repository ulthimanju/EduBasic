-- 0. Add current_version to exams
ALTER TABLE exams ADD COLUMN current_version INT DEFAULT 1;

-- 1. Snapshot Table for Published Exams
CREATE TABLE exam_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID NOT NULL REFERENCES exams(id),
    version INT NOT NULL,
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(exam_id, version)
);

-- 2. Link Attempts to Snapshots instead of just Exams
ALTER TABLE student_attempts ADD COLUMN exam_snapshot_id UUID REFERENCES exam_snapshots(id);

-- 3. Add an index for faster snapshot lookups
CREATE INDEX idx_exam_snapshots_exam_version ON exam_snapshots(exam_id, version);

ALTER TABLE exam_sessions ADD COLUMN last_activity_at TIMESTAMP DEFAULT now();
ALTER TABLE exam_sessions ADD COLUMN warning_message TEXT;

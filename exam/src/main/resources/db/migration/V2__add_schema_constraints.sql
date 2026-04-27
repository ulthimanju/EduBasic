-- Add CHECK constraints for enum columns
ALTER TABLE questions 
    ADD CONSTRAINT check_question_type CHECK (type IN ('MCQ_SINGLE', 'MCQ_MULTI', 'TRUE_FALSE', 'FILL_BLANK', 'MATCH', 'SEQUENCE', 'CODING', 'SUBJECTIVE')),
    ADD CONSTRAINT check_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'));

ALTER TABLE exams 
    ADD CONSTRAINT check_exam_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));

ALTER TABLE student_attempts 
    ADD CONSTRAINT check_attempt_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EVALUATED'));

ALTER TABLE student_answers 
    ADD CONSTRAINT check_evaluation_status CHECK (evaluation_status IN ('PENDING', 'AUTO_GRADED', 'MANUAL_GRADED', 'PENDING_SANDBOX', 'PENDING_MANUAL', 'AUTO_GRADED_CODING', 'AUTO_GRADED_CORRECT', 'AUTO_GRADED_INCORRECT'));

ALTER TABLE evaluation_results 
    ADD CONSTRAINT check_result_status CHECK (status IN ('PENDING_AUTO', 'PENDING_MANUAL', 'COMPLETED'));

ALTER TABLE proctoring_logs
    ADD CONSTRAINT check_violation_type CHECK (violation_type IN ('TAB_SWITCH', 'FULLSCREEN_EXIT', 'SHORTCUT_BLOCKED', 'PASTE_ATTEMPT'));

-- Add NOT NULL constraints for required Foreign Keys
-- Note: Using subqueries or ensuring data exists might be needed if tables weren't empty, 
-- but in many development cycles these are safe.

ALTER TABLE exam_sections ALTER COLUMN exam_id SET NOT NULL;

ALTER TABLE exam_question_mapping 
    ALTER COLUMN exam_id SET NOT NULL,
    ALTER COLUMN question_id SET NOT NULL;

ALTER TABLE student_attempts ALTER COLUMN exam_id SET NOT NULL;

ALTER TABLE student_answers 
    ALTER COLUMN attempt_id SET NOT NULL,
    ALTER COLUMN question_id SET NOT NULL;

ALTER TABLE evaluation_results ALTER COLUMN attempt_id SET NOT NULL;

ALTER TABLE certificates ALTER COLUMN attempt_id SET NOT NULL;

ALTER TABLE proctoring_logs ALTER COLUMN attempt_id SET NOT NULL;

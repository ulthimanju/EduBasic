-- Performance Indexes for hot query patterns

-- Student Attempts: filter/join by student and exam with status for fast lookup of in-progress attempts
CREATE INDEX idx_student_attempts_lookup ON student_attempts(student_id, exam_id, status);

-- Student Answers: lookup by attempt_id and question_id for fast sync/updates
CREATE INDEX idx_student_answers_composite ON student_answers(attempt_id, question_id);

-- Exam Question Mapping: join/filter by exam and section with order index for fast ordered fetches
CREATE INDEX idx_exam_mapping_exam_ordered ON exam_question_mapping(exam_id, order_index);
CREATE INDEX idx_exam_mapping_section_ordered ON exam_question_mapping(section_id, order_index);

-- Proctoring Logs: filter by attempt_id
CREATE INDEX idx_proctoring_logs_attempt ON proctoring_logs(attempt_id);

-- Question Bank: search by type and difficulty
CREATE INDEX idx_questions_type_difficulty ON questions(type, difficulty);

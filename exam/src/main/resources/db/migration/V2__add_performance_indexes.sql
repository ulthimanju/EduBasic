-- Performance Indexes for hot query patterns

-- Student Attempts: filter/join by student and exam
CREATE INDEX idx_student_attempts_student_exam ON student_attempts(student_id, exam_id);

-- Student Answers: lookup by attempt_id (evaluation, sync)
CREATE INDEX idx_student_answers_attempt ON student_answers(attempt_id);

-- Exam Question Mapping: join/filter by exam and section (exam build, evaluation)
CREATE INDEX idx_exam_mapping_exam ON exam_question_mapping(exam_id);
CREATE INDEX idx_exam_mapping_section ON exam_question_mapping(section_id);

-- Proctoring Logs: filter by attempt_id
CREATE INDEX idx_proctoring_logs_attempt ON proctoring_logs(attempt_id);

-- Question Bank: search by type and difficulty
CREATE INDEX idx_questions_type_difficulty ON questions(type, difficulty);

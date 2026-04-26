-- Question Types Enum
-- MCQ_SINGLE, MCQ_MULTI, TRUE_FALSE, FILL_BLANK, MATCH, SEQUENCE, CODING, SUBJECTIVE

-- Difficulty Enum
-- EASY, MEDIUM, HARD

-- Exam Status Enum
-- DRAFT, PUBLISHED, ARCHIVED

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    payload JSONB NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    tags TEXT[],
    default_marks DECIMAL(10,2) DEFAULT 1.0,
    default_neg_mark DECIMAL(10,2) DEFAULT 0.0,
    is_public BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    has_sections BOOLEAN DEFAULT FALSE,
    time_limit_mins INTEGER,
    shuffle_questions BOOLEAN DEFAULT TRUE,
    shuffle_options BOOLEAN DEFAULT TRUE,
    allow_backtrack BOOLEAN DEFAULT TRUE,
    max_attempts INTEGER DEFAULT 1,
    pass_marks DECIMAL(10,2),
    negative_marking BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'DRAFT',
    proctoring_enabled BOOLEAN DEFAULT FALSE,
    max_violations INTEGER DEFAULT 3,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exam_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID REFERENCES exams(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE exam_question_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID REFERENCES exams(id) ON DELETE CASCADE,
    section_id UUID REFERENCES exam_sections(id) ON DELETE CASCADE,
    question_id UUID REFERENCES questions(id),
    order_index INTEGER NOT NULL,
    marks DECIMAL(10,2) NOT NULL,
    neg_mark DECIMAL(10,2) DEFAULT 0.0,
    is_mandatory BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Note: student_attempts, student_answers, evaluation_results etc. are kept for future phases
CREATE TABLE student_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    exam_id UUID REFERENCES exams(id),
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP WITH TIME ZONE,
    score DECIMAL(10,2),
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE student_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID REFERENCES student_attempts(id),
    question_id UUID REFERENCES questions(id),
    raw_answer TEXT,
    evaluation_status VARCHAR(50) NOT NULL,
    marks_obtained DECIMAL(10,2),
    feedback TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(attempt_id, question_id)
);

CREATE TABLE evaluation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID REFERENCES student_attempts(id) UNIQUE,
    status VARCHAR(50) NOT NULL,
    total_score DECIMAL(10,2),
    result_json JSONB,
    evaluated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID REFERENCES student_attempts(id) UNIQUE,
    certificate_url VARCHAR(500),
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE proctoring_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID REFERENCES student_attempts(id),
    violation_type VARCHAR(100), -- TAB_SWITCH, FULLSCREEN_EXIT, SHORTCUT_BLOCKED, PASTE_ATTEMPT
    metadata JSONB,
    captured_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_name VARCHAR(100),
    entity_id UUID,
    action VARCHAR(50),
    performed_by UUID,
    payload JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

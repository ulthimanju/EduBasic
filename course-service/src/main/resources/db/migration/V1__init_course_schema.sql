CREATE TABLE courses (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by        UUID NOT NULL,
    title             TEXT NOT NULL,
    description       TEXT,
    thumbnail_url     TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    completion_rules  JSONB NOT NULL DEFAULT '{"requireAllLessons":true,"requireAllExams":true,"minPassPercent":70}',
    is_deleted        BOOLEAN NOT NULL DEFAULT false,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE course_modules (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title        TEXT NOT NULL,
    description  TEXT,
    order_index  INT NOT NULL,
    is_deleted   BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (course_id, order_index)
);

CREATE TABLE lessons (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id         UUID NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    title             TEXT NOT NULL,
    content_type      VARCHAR(20) NOT NULL
                          CHECK (content_type IN ('TEXT','LINK','ASSIGNMENT')),
    content_body      TEXT,
    content_url       TEXT,
    duration_minutes  INT,
    order_index       INT NOT NULL,
    is_preview        BOOLEAN NOT NULL DEFAULT false,
    is_deleted        BOOLEAN NOT NULL DEFAULT false,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (module_id, order_index)
);

CREATE TABLE course_enrollments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID NOT NULL REFERENCES courses(id),
    student_id   UUID NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE','COMPLETED','DROPPED')),
    enrolled_at  TIMESTAMP NOT NULL DEFAULT now(),
    completed_at TIMESTAMP,
    UNIQUE (course_id, student_id)
);

CREATE TABLE lesson_progress (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id        UUID NOT NULL REFERENCES lessons(id),
    student_id       UUID NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
                         CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    progress_percent INT NOT NULL DEFAULT 0
                         CHECK (progress_percent BETWEEN 0 AND 100),
    last_accessed_at TIMESTAMP,
    completed_at     TIMESTAMP,
    UNIQUE (lesson_id, student_id)
);

CREATE TABLE course_exams (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id             UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    exam_id               UUID NOT NULL,
    title                 TEXT NOT NULL,
    order_index           INT NOT NULL,
    required_to_complete  BOOLEAN NOT NULL DEFAULT true,
    min_pass_percent      INT CHECK (min_pass_percent BETWEEN 0 AND 100),
    UNIQUE (course_id, exam_id)
);

CREATE TABLE course_completion_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID NOT NULL REFERENCES courses(id),
    student_id   UUID NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT now(),
    trigger_type VARCHAR(20) NOT NULL CHECK (trigger_type IN ('AUTO','MANUAL')),
    snapshot     JSONB NOT NULL,
    UNIQUE (course_id, student_id)
);

CREATE INDEX idx_courses_created_by       ON courses(created_by);
CREATE INDEX idx_courses_status           ON courses(status);
CREATE INDEX idx_modules_course_id        ON course_modules(course_id);
CREATE INDEX idx_lessons_module_id        ON lessons(module_id);
CREATE INDEX idx_enrollments_student_id   ON course_enrollments(student_id);
CREATE INDEX idx_enrollments_course_id    ON course_enrollments(course_id);
CREATE INDEX idx_progress_student_id      ON lesson_progress(student_id);
CREATE INDEX idx_course_exams_course_id   ON course_exams(course_id);

-- courses
CREATE TABLE courses (
  id UUID PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  topics JSONB,
  created_at TIMESTAMP DEFAULT now()
);

-- exam_sessions
CREATE TABLE exam_sessions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  course_id UUID REFERENCES courses(id),
  status VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE | COMPLETED | ABANDONED
  current_index INT DEFAULT 0,
  current_difficulty VARCHAR(20) DEFAULT 'MEDIUM',
  streak INT DEFAULT 0,
  started_at TIMESTAMP,
  completed_at TIMESTAMP
);

-- questions (fallback bank)
CREATE TABLE questions (
  id UUID PRIMARY KEY,
  course_id UUID REFERENCES courses(id),
  question TEXT NOT NULL,
  options JSONB NOT NULL,
  correct_answer VARCHAR(255),
  explanation TEXT,
  topic VARCHAR(100),
  difficulty VARCHAR(20),
  source VARCHAR(20) DEFAULT 'FALLBACK'  -- GEMINI | FALLBACK
);

-- user_answers
CREATE TABLE user_answers (
  id UUID PRIMARY KEY,
  session_id UUID REFERENCES exam_sessions(id),
  question_id UUID,
  selected_option VARCHAR(255),
  is_correct BOOLEAN,
  time_taken INT,
  answered_at TIMESTAMP
);

-- exam_results
CREATE TABLE exam_results (
  id UUID PRIMARY KEY,
  session_id UUID REFERENCES exam_sessions(id),
  user_id UUID NOT NULL,
  course_id UUID,
  level VARCHAR(20),
  raw_score FLOAT,
  normalized_score FLOAT,
  topics_strong JSONB,
  topics_weak JSONB,
  difficulty_breakdown JSONB,
  created_at TIMESTAMP
);

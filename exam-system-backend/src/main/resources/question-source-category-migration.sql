USE exam_system;

ALTER TABLE question
  ADD COLUMN source_category VARCHAR(30) NULL AFTER knowledge_tag;

UPDATE question
SET source_category = 'REAL_EXAM'
WHERE exam_year IS NOT NULL AND (source_category IS NULL OR source_category = '');

CREATE INDEX idx_question_source_category ON question(source_category, exam_year, exam_scope, province);

USE exam_system;

ALTER TABLE student_answer
  ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'AUTO_GRADED' AFTER is_correct,
  ADD COLUMN review_comment VARCHAR(500) NULL AFTER review_status,
  ADD COLUMN reviewer_id BIGINT NULL AFTER review_comment,
  ADD COLUMN review_time DATETIME NULL AFTER reviewer_id;

UPDATE student_answer sa
JOIN question q ON q.id = sa.question_id
SET sa.review_status = CASE
  WHEN q.question_type = 'SHORT_ANSWER' AND sa.is_correct IS NULL THEN 'PENDING_REVIEW'
  ELSE 'AUTO_GRADED'
END;

UPDATE student_exam se
SET se.status = CASE
  WHEN EXISTS (
    SELECT 1 FROM student_answer sa
    WHERE sa.student_exam_id = se.id
      AND sa.review_status = 'PENDING_REVIEW'
  ) THEN 'PENDING_REVIEW'
  WHEN se.submit_time IS NOT NULL THEN 'SUBMITTED'
  ELSE se.status
END;

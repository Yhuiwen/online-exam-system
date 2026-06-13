USE exam_system;

CREATE TABLE IF NOT EXISTS exam_violation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  violation_type VARCHAR(50) NOT NULL,
  description VARCHAR(500),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_violation_exam(exam_id),
  INDEX idx_violation_student_exam(student_exam_id),
  INDEX idx_violation_deduplicate(student_exam_id, violation_type, create_time)
);

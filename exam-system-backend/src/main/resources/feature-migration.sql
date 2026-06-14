USE exam_system;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  real_name VARCHAR(50),
  module VARCHAR(50) NOT NULL,
  action VARCHAR(100) NOT NULL,
  method VARCHAR(10) NOT NULL,
  path VARCHAR(200) NOT NULL,
  ip VARCHAR(64),
  detail VARCHAR(1000),
  success TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_operation_log_time(create_time),
  INDEX idx_operation_log_user(user_id)
);

CREATE TABLE IF NOT EXISTS exam_proctor (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exam_proctor(exam_id, teacher_id),
  INDEX idx_exam_proctor_teacher(teacher_id)
);

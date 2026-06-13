CREATE DATABASE IF NOT EXISTS exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_system;

DROP TABLE IF EXISTS wrong_question;
DROP TABLE IF EXISTS student_answer;
DROP TABLE IF EXISTS student_exam;
DROP TABLE IF EXISTS exam_question;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  role VARCHAR(20) NOT NULL,
  email VARCHAR(100),
  phone VARCHAR(20),
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  teacher_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
);
CREATE TABLE question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  question_type VARCHAR(30) NOT NULL,
  content TEXT NOT NULL,
  options_json JSON,
  answer TEXT NOT NULL,
  analysis TEXT,
  difficulty VARCHAR(20) NOT NULL,
  score DECIMAL(8,2) NOT NULL,
  knowledge_tag VARCHAR(100),
  create_user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_question_filter(course_id, question_type, difficulty)
);
CREATE TABLE exam (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_name VARCHAR(100) NOT NULL,
  course_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  start_time DATETIME,
  end_time DATETIME,
  duration_minutes INT NOT NULL,
  total_score DECIMAL(8,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE exam_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  sort_no INT NOT NULL,
  score DECIMAL(8,2) NOT NULL,
  UNIQUE KEY uk_exam_question(exam_id, question_id)
);
CREATE TABLE student_exam (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  submit_time DATETIME,
  total_score DECIMAL(8,2) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL,
  UNIQUE KEY uk_student_exam(student_id, exam_id)
);
CREATE TABLE student_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_exam_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer TEXT,
  score DECIMAL(8,2),
  is_correct TINYINT,
  review_status VARCHAR(20) NOT NULL,
  review_comment VARCHAR(500),
  reviewer_id BIGINT,
  review_time DATETIME,
  UNIQUE KEY uk_student_answer(student_exam_id, question_id)
);
CREATE TABLE wrong_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  exam_id BIGINT NOT NULL,
  student_answer TEXT,
  correct_answer TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

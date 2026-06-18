USE exam_system;

CREATE TABLE IF NOT EXISTS course_knowledge_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  original_filename VARCHAR(255),
  file_type VARCHAR(20),
  file_size BIGINT,
  chunk_count INT NOT NULL DEFAULT 0,
  create_user_id BIGINT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_course_id(course_id)
);

CREATE TABLE IF NOT EXISTS course_knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  content_hash VARCHAR(64),
  embedding_json TEXT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_document_id(document_id),
  INDEX idx_course_id(course_id),
  INDEX idx_course_chunk(course_id, document_id, chunk_index)
);

-- 一次性修复：UTF-8 文本被当作 latin1 写入导致的乱码
-- 仅对已有乱码库执行一次；新库请用 00-charset.sql + UTF-8 脚本初始化，无需执行本文件
USE exam_system;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE sys_user SET
  real_name = CONVERT(CAST(CONVERT(real_name USING latin1) AS BINARY) USING utf8mb4)
WHERE real_name IS NOT NULL AND real_name <> '';

UPDATE course SET
  course_name = CONVERT(CAST(CONVERT(course_name USING latin1) AS BINARY) USING utf8mb4),
  description = CONVERT(CAST(CONVERT(description USING latin1) AS BINARY) USING utf8mb4)
WHERE course_name IS NOT NULL;

UPDATE question SET
  content = CONVERT(CAST(CONVERT(content USING latin1) AS BINARY) USING utf8mb4),
  answer = CONVERT(CAST(CONVERT(answer USING latin1) AS BINARY) USING utf8mb4),
  analysis = CONVERT(CAST(CONVERT(analysis USING latin1) AS BINARY) USING utf8mb4),
  knowledge_tag = CONVERT(CAST(CONVERT(knowledge_tag USING latin1) AS BINARY) USING utf8mb4)
WHERE content IS NOT NULL;

UPDATE exam SET
  exam_name = CONVERT(CAST(CONVERT(exam_name USING latin1) AS BINARY) USING utf8mb4)
WHERE exam_name IS NOT NULL;

UPDATE exam_violation SET
  description = CONVERT(CAST(CONVERT(description USING latin1) AS BINARY) USING utf8mb4)
WHERE description IS NOT NULL AND description <> '';

UPDATE operation_log SET
  real_name = CONVERT(CAST(CONVERT(real_name USING latin1) AS BINARY) USING utf8mb4),
  action = CONVERT(CAST(CONVERT(action USING latin1) AS BINARY) USING utf8mb4),
  detail = CONVERT(CAST(CONVERT(detail USING latin1) AS BINARY) USING utf8mb4)
WHERE id IS NOT NULL;

UPDATE course_knowledge_document SET
  title = CONVERT(CAST(CONVERT(title USING latin1) AS BINARY) USING utf8mb4),
  original_filename = CONVERT(CAST(CONVERT(original_filename USING latin1) AS BINARY) USING utf8mb4)
WHERE title IS NOT NULL;

UPDATE course_knowledge_chunk SET
  content = CONVERT(CAST(CONVERT(content USING latin1) AS BINARY) USING utf8mb4)
WHERE content IS NOT NULL;

UPDATE student_answer SET
  answer = CONVERT(CAST(CONVERT(answer USING latin1) AS BINARY) USING utf8mb4),
  review_comment = CONVERT(CAST(CONVERT(review_comment USING latin1) AS BINARY) USING utf8mb4)
WHERE answer IS NOT NULL OR review_comment IS NOT NULL;

UPDATE wrong_question SET
  student_answer = CONVERT(CAST(CONVERT(student_answer USING latin1) AS BINARY) USING utf8mb4),
  correct_answer = CONVERT(CAST(CONVERT(correct_answer USING latin1) AS BINARY) USING utf8mb4)
WHERE student_answer IS NOT NULL;

ALTER DATABASE exam_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

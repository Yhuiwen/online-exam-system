USE exam_system;

-- 已有库增量执行；新库已在 ai-knowledge-migration.sql 中包含该列
ALTER TABLE course_knowledge_chunk
  ADD COLUMN embedding_json TEXT NULL COMMENT '向量 embedding JSON 数组';

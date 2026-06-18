package com.exam.system.ai.embedding;

import com.exam.system.ai.config.AiProperties;
import com.exam.system.ai.knowledge.entity.CourseKnowledgeChunk;
import com.exam.system.ai.knowledge.mapper.CourseKnowledgeChunkMapper;
import com.exam.system.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChunkEmbeddingService {
    private final EmbeddingClient embeddingClient;
    private final CourseKnowledgeChunkMapper chunkMapper;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    public float[] embedText(String text) {
        return embeddingClient.embed(text);
    }

    public float[] embeddingOf(CourseKnowledgeChunk chunk) {
        if (chunk == null) return null;
        float[] cached = VectorUtils.fromJson(chunk.getEmbeddingJson(), objectMapper);
        if (cached != null && cached.length > 0) return cached;
        if (!aiProperties.getEmbedding().isEnabled()) return null;
        float[] vector = embeddingClient.embed(chunk.getContent());
        chunk.setEmbeddingJson(VectorUtils.toJson(vector, objectMapper));
        chunkMapper.updateById(chunk);
        return vector;
    }

    public void saveEmbedding(CourseKnowledgeChunk chunk) {
        if (chunk == null || chunk.getContent() == null || chunk.getContent().isBlank()) return;
        if (!aiProperties.getEmbedding().isEnabled()) return;
        try {
            float[] vector = embeddingClient.embed(chunk.getContent());
            chunk.setEmbeddingJson(VectorUtils.toJson(vector, objectMapper));
        } catch (BusinessException e) {
            // 上传不因 embedding 失败而中断，检索时回退关键词模式
        }
    }
}

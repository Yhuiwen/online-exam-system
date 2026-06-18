package com.exam.system.ai.knowledge.vo;

public record KnowledgeUploadResponse(
        Long documentId,
        Long courseId,
        String title,
        String originalFilename,
        String fileType,
        Long fileSize,
        Integer chunkCount
) {
}

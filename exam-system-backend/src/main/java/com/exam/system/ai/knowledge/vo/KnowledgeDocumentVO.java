package com.exam.system.ai.knowledge.vo;

import java.time.LocalDateTime;

public record KnowledgeDocumentVO(
        Long id,
        Long courseId,
        String title,
        String originalFilename,
        String fileType,
        Long fileSize,
        Integer chunkCount,
        LocalDateTime createTime
) {
}

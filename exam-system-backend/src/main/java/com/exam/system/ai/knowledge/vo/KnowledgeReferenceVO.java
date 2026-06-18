package com.exam.system.ai.knowledge.vo;

public record KnowledgeReferenceVO(
        Long documentId,
        String documentTitle,
        Integer chunkIndex,
        String contentPreview,
        Double score
) {
}

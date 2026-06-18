package com.exam.system.ai.knowledge.vo;

import java.util.List;

public record KnowledgeAskResponse(
        String answer,
        List<KnowledgeReferenceVO> references
) {
}

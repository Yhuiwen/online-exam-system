package com.exam.system.ai.knowledge.service;

import com.exam.system.ai.knowledge.dto.KnowledgeAskRequest;
import com.exam.system.ai.knowledge.vo.KnowledgeAskResponse;

public interface KnowledgeQaService {
    KnowledgeAskResponse ask(KnowledgeAskRequest request);
}

package com.exam.system.ai.knowledge.service;

import com.exam.system.ai.knowledge.vo.KnowledgeDocumentVO;
import com.exam.system.ai.knowledge.vo.KnowledgeUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocumentService {
    KnowledgeUploadResponse upload(Long courseId, String title, MultipartFile file);

    List<KnowledgeDocumentVO> list(Long courseId);

    void delete(Long id);
}

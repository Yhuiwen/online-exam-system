package com.exam.system.ai.service;

import com.exam.system.ai.dto.AiPaperGenerateRequest;
import com.exam.system.ai.dto.AiQuestionGenerateRequest;
import com.exam.system.ai.dto.AiQuestionSaveRequest;
import com.exam.system.ai.vo.AiGeneratedQuestionVO;
import com.exam.system.vo.PaperPreviewVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiQuestionService {
    List<AiGeneratedQuestionVO> generate(AiQuestionGenerateRequest request);

    void saveGeneratedQuestions(AiQuestionSaveRequest request);

    PaperPreviewVO generatePaper(AiPaperGenerateRequest request);

    List<AiGeneratedQuestionVO> parseDocument(Long courseId, MultipartFile file, String knowledgePoint);
}

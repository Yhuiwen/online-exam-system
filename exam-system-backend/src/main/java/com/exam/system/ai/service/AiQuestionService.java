package com.exam.system.ai.service;

import com.exam.system.ai.dto.AiQuestionGenerateRequest;
import com.exam.system.ai.dto.AiQuestionSaveRequest;
import com.exam.system.ai.vo.AiGeneratedQuestionVO;

import java.util.List;

public interface AiQuestionService {
    List<AiGeneratedQuestionVO> generate(AiQuestionGenerateRequest request);

    void saveGeneratedQuestions(AiQuestionSaveRequest request);
}

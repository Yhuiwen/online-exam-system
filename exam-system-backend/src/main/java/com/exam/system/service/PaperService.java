package com.exam.system.service;

import com.exam.system.dto.ManualPaperSaveRequest;
import com.exam.system.dto.ManualPaperUpdateRequest;
import com.exam.system.vo.ManualQuestionVO;
import com.exam.system.vo.PaperPreviewVO;

import java.util.List;

public interface PaperService {
    List<ManualQuestionVO> selectableQuestions(Long examId, String questionType, String difficulty,
                                               String keyword, String knowledgeTag);

    PaperPreviewVO saveManualPaper(Long examId, ManualPaperSaveRequest request);

    PaperPreviewVO preview(Long examId);

    PaperPreviewVO deleteQuestion(Long examId, Long questionId);

    PaperPreviewVO updateQuestion(Long examId, Long questionId, ManualPaperUpdateRequest request);
}

package com.exam.system.service;

import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.Question;
import java.util.List;

public interface ExamService {
    List<Question> autoPaper(Long examId, AutoPaperRequest request);
    List<Question> questions(Long examId, boolean includeAnswers);
}

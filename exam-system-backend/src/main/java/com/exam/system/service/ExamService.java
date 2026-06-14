package com.exam.system.service;

import com.exam.system.dto.AssignExamProctorsRequest;
import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.Question;
import com.exam.system.vo.ExamVO;

import java.util.List;

public interface ExamService {
    List<ExamVO> listExams();

    List<ExamVO> listMonitorableExams();

    ExamVO getExamVO(Long examId);

    List<Question> autoPaper(Long examId, AutoPaperRequest request);

    List<Question> questions(Long examId, boolean includeAnswers);

    void deleteExam(Long examId);

    void assignProctors(Long examId, AssignExamProctorsRequest request);
}

package com.exam.system.service;

import com.exam.system.dto.AssignExamProctorsRequest;
import com.exam.system.dto.AutoPaperRequest;
import com.exam.system.entity.Exam;
import com.exam.system.entity.Question;
import com.exam.system.vo.ExamVO;

import java.util.List;

public interface ExamService {
    List<ExamVO> listExams();

    List<ExamVO> listMonitorableExams();

    Exam createExam(Exam exam);

    ExamVO getExamVO(Long examId);

    void updateExam(Long examId, Exam patch);

    void updateStatus(Long examId, String status);

    void addQuestion(Long examId, Long questionId);

    List<Question> autoPaper(Long examId, AutoPaperRequest request);

    List<Question> questions(Long examId);

    void deleteExam(Long examId);

    void assignProctors(Long examId, AssignExamProctorsRequest request);
}

package com.exam.system.service;

import com.exam.system.dto.ExamViolationReportRequest;
import com.exam.system.vo.ExamViolationSummaryVO;
import com.exam.system.vo.ExamViolationVO;

import java.util.List;

public interface ExamViolationService {
    ExamViolationVO report(ExamViolationReportRequest request);

    List<ExamViolationSummaryVO> examSummary(Long examId);

    List<ExamViolationVO> studentExamDetails(Long studentExamId);

    ExamViolationSummaryVO mySummary(Long studentExamId);
}

package com.exam.system.vo;

import java.time.LocalDateTime;

public record ExamViolationSummaryVO(
        Long studentExamId,
        Long studentId,
        String studentName,
        long violationCount,
        String riskLevel,
        LocalDateTime lastViolationTime) {
}

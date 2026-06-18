package com.exam.system.vo;

import java.time.LocalDateTime;

public record ExamViolationSummaryVO(
        Long studentExamId,
        Long studentId,
        String studentName,
        long violationCount,
        String riskLevel,
        int riskScore,
        long pageHiddenCount,
        long windowBlurCount,
        long fullscreenExitCount,
        long copyCount,
        long pasteCount,
        long rightClickCount,
        long devtoolsCount,
        LocalDateTime lastViolationTime) {
}

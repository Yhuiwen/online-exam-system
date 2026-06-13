package com.exam.system.vo;

import java.time.LocalDateTime;

public record ExamViolationVO(
        Long id,
        Long studentExamId,
        Long studentId,
        Long examId,
        String violationType,
        String description,
        LocalDateTime createTime) {
}

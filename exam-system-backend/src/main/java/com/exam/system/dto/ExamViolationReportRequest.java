package com.exam.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExamViolationReportRequest(
        @NotNull(message = "学生考试记录ID不能为空")
        Long studentExamId,
        @NotNull(message = "考试ID不能为空")
        Long examId,
        @NotBlank(message = "异常类型不能为空")
        String violationType,
        @Size(max = 500, message = "异常描述不能超过500字")
        String description) {
}

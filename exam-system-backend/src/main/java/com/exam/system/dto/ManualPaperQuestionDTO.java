package com.exam.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ManualPaperQuestionDTO(
        @NotNull(message = "题目ID不能为空")
        Long questionId,
        @NotNull(message = "题目分值不能为空")
        @DecimalMin(value = "0.01", message = "题目分值必须大于 0")
        BigDecimal score,
        @NotNull(message = "题目顺序不能为空")
        @Min(value = 1, message = "题目顺序必须大于 0")
        Integer sortNo) {
}

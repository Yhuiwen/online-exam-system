package com.exam.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReviewAnswerRequest(
        @NotNull(message = "批改分数不能为空")
        @DecimalMin(value = "0.0", message = "批改分数不能小于 0")
        BigDecimal score,
        @Size(max = 500, message = "批改评语不能超过 500 字")
        String reviewComment) {
}

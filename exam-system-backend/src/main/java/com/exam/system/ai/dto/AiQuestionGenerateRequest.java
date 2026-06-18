package com.exam.system.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiQuestionGenerateRequest {
    @NotNull(message = "课程不能为空")
    private Long courseId;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    private String knowledgePoint;

    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量必须在 1 到 20 之间")
    @Max(value = 20, message = "生成数量必须在 1 到 20 之间")
    private Integer count;

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "分值必须大于 0")
    private BigDecimal score;

    private String requirement;
}

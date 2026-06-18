package com.exam.system.ai.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiGeneratedQuestionVO {
    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotBlank(message = "题干不能为空")
    private String content;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    @NotBlank(message = "答案不能为空")
    private String correctAnswer;

    @NotBlank(message = "解析不能为空")
    private String analysis;

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @NotNull(message = "分值不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "分值必须大于 0")
    private BigDecimal score;

    private String knowledgePoint;
}

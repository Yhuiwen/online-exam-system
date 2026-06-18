package com.exam.system.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiPaperGenerateRequest {
    @NotNull(message = "考试不能为空")
    private Long examId;

    @NotEmpty(message = "组卷规则不能为空")
    @Valid
    private List<AiPaperSectionDTO> sections;
}

package com.exam.system.ai.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeAskRequest {
    @NotNull(message = "课程不能为空")
    private Long courseId;

    @NotBlank(message = "问题不能为空")
    private String question;

    @Min(value = 1, message = "topK 必须在 1 到 8 之间")
    @Max(value = 8, message = "topK 必须在 1 到 8 之间")
    private Integer topK = 5;
}

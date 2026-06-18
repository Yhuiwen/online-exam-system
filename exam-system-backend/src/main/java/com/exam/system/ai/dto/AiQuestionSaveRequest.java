package com.exam.system.ai.dto;

import com.exam.system.ai.vo.AiGeneratedQuestionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiQuestionSaveRequest {
    @NotNull(message = "课程不能为空")
    private Long courseId;

    @Valid
    @NotEmpty(message = "请先生成并确认题目")
    @Size(max = 20, message = "一次最多保存 20 道题")
    private List<AiGeneratedQuestionVO> questions;
}

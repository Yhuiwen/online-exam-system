package com.exam.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ManualPaperSaveRequest(
        Long examId,
        @NotNull(message = "题目列表不能为空")
        List<@Valid ManualPaperQuestionDTO> questions) {
}

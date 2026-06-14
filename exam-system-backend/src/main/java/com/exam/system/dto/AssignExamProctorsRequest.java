package com.exam.system.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignExamProctorsRequest(
        @NotNull(message = "监考教师列表不能为空")
        @NotEmpty(message = "至少分配一名监考教师")
        List<Long> teacherIds
) {
}

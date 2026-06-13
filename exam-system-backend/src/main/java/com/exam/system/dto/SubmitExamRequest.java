package com.exam.system.dto;
import jakarta.validation.constraints.NotNull;
import java.util.List;
public record SubmitExamRequest(@NotNull Long studentExamId, List<AnswerItem> answers) {
    public record AnswerItem(Long questionId, String answer) {}
}

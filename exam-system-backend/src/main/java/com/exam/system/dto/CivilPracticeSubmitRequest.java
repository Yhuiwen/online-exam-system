package com.exam.system.dto;

import java.util.List;

public record CivilPracticeSubmitRequest(
        String moduleCode,
        String moduleName,
        Integer durationSeconds,
        List<AnswerItem> answers
) {
    public record AnswerItem(Long questionId, String answer, Integer durationSeconds) {}
}

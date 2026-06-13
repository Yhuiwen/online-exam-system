package com.exam.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AutoPaperRequest(
        @NotNull Long courseId,
        @Min(0) int singleChoiceCount,
        @Min(0) int multipleChoiceCount,
        @Min(0) int trueFalseCount,
        @Min(0) int fillBlankCount,
        @Min(0) int shortAnswerCount,
        double easyRatio,
        double mediumRatio,
        double hardRatio) {}

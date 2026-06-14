package com.exam.system.dto;

public record CivilDrillAnswerRequest(
        Long sessionId,
        Long questionId,
        String answer,
        Integer durationSeconds
) {}

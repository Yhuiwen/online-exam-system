package com.exam.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaperPreviewVO(
        Long examId,
        String examName,
        Long courseId,
        String courseName,
        Integer durationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal totalScore,
        int questionCount,
        List<PaperPreviewQuestionVO> questions) {
}

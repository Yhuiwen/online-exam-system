package com.exam.system.vo;

import java.time.LocalDateTime;

public record StudentExamSessionVO(
        Long studentExamId,
        Long examId,
        String examName,
        Integer durationMinutes,
        LocalDateTime startTime,
        LocalDateTime deadline,
        LocalDateTime serverTime,
        long remainingSeconds,
        boolean timedOut,
        String status
) {
}

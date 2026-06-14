package com.exam.system.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExamVO(
        Long id,
        String examName,
        Long courseId,
        Long teacherId,
        String teacherName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer durationMinutes,
        BigDecimal totalScore,
        String status,
        List<ProctorVO> proctors,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public record ProctorVO(Long teacherId, String teacherName) {
    }
}

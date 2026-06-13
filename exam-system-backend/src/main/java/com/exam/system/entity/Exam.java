package com.exam.system.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Exam extends BaseEntity {
    private Long id;
    private String examName;
    private Long courseId;
    private Long teacherId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private BigDecimal totalScore;
    private String status;
}

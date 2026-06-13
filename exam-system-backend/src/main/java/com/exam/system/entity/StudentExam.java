package com.exam.system.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentExam {
    private Long id;
    private Long studentId;
    private Long examId;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private BigDecimal totalScore;
    private String status;
}

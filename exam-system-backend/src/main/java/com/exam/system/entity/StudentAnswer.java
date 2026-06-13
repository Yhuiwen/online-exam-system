package com.exam.system.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentAnswer {
    private Long id;
    private Long studentExamId;
    private Long questionId;
    private String answer;
    private BigDecimal score;
    private Boolean isCorrect;
    private String reviewStatus;
    private String reviewComment;
    private Long reviewerId;
    private LocalDateTime reviewTime;
}

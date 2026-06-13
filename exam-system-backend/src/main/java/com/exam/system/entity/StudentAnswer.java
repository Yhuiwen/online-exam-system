package com.exam.system.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentAnswer {
    private Long id;
    private Long studentExamId;
    private Long questionId;
    private String answer;
    private BigDecimal score;
    private Boolean isCorrect;
}

package com.exam.system.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExamQuestion {
    private Long id;
    private Long examId;
    private Long questionId;
    private Integer sortNo;
    private BigDecimal score;
}

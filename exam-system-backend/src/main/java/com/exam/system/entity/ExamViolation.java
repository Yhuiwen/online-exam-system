package com.exam.system.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamViolation {
    private Long id;
    private Long studentExamId;
    private Long studentId;
    private Long examId;
    private String violationType;
    private String description;
    private LocalDateTime createTime;
}

package com.exam.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WrongQuestion {
    private Long id;
    private Long studentId;
    private Long questionId;
    private Long examId;
    private String studentAnswer;
    private String correctAnswer;
    private LocalDateTime createTime;
}

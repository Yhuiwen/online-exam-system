package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("civil_practice_answer")
public class CivilPracticeAnswer {
    private Long id;
    private Long sessionId;
    private Long studentId;
    private Long questionId;
    private String moduleCode;
    private String userAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Integer durationSeconds;
    private LocalDateTime createTime;
}

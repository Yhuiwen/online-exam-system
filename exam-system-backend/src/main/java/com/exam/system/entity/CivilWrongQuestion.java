package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("civil_wrong_question")
public class CivilWrongQuestion extends BaseEntity {
    private Long id;
    private Long studentId;
    private Long questionId;
    private String moduleCode;
    private String userAnswer;
    private String correctAnswer;
    private Integer wrongCount;
    private Integer mastered;
    private LocalDateTime lastWrongTime;
}

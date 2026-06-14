package com.exam.system.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamProctor {
    private Long id;
    private Long examId;
    private Long teacherId;
    private LocalDateTime createTime;
}

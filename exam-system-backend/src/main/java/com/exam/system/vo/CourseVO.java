package com.exam.system.vo;

import java.time.LocalDateTime;

public record CourseVO(
        Long id,
        String courseName,
        String description,
        Long teacherId,
        String teacherName,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}

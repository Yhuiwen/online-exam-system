package com.exam.system.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Course extends BaseEntity {
    private Long id;
    private String courseName;
    private String description;
    private Long teacherId;
}

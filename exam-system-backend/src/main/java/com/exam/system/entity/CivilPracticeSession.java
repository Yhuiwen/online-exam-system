package com.exam.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("civil_practice_session")
public class CivilPracticeSession extends BaseEntity {
    private Long id;
    private Long studentId;
    private String moduleCode;
    private String moduleName;
    private Integer questionCount;
    private Integer correctCount;
    private BigDecimal accuracy;
    private Integer durationSeconds;
}

package com.exam.system.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Question extends BaseEntity {
    private Long id;
    private Long courseId;
    private String questionType;
    private String content;
    private String optionsJson;
    private String answer;
    private String analysis;
    private String difficulty;
    private BigDecimal score;
    private String knowledgeTag;
    private Long createUserId;
}

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
  private String sourceCategory;
  private Integer examYear;
  private String examScope;
  private String province;
  private String paperType;
  private String sourceRef;
  private Long createUserId;
}

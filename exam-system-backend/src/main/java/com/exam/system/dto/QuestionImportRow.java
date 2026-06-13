package com.exam.system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionImportRow {
    private Long courseId;
    private String questionType;
    private String content;
    private List<String> options;
    private String answer;
    private String analysis;
    private String difficulty;
    private BigDecimal score;
    private String knowledgeTag;
}

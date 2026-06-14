package com.exam.system.vo;

import java.math.BigDecimal;

public record ManualQuestionVO(
        Long questionId,
        String questionType,
        String content,
        String optionsJson,
        String answer,
        String analysis,
        String difficulty,
        BigDecimal defaultScore,
        String knowledgeTag,
        boolean selected,
        BigDecimal selectedScore,
        Integer sortNo) {
}

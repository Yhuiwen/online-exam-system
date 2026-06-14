package com.exam.system.vo;

import java.math.BigDecimal;

public record PaperPreviewQuestionVO(
        Long questionId,
        String questionType,
        String content,
        String optionsJson,
        String answer,
        String analysis,
        String difficulty,
        BigDecimal score,
        Integer sortNo,
        String knowledgeTag) {
}

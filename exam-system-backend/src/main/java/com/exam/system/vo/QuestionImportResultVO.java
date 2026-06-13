package com.exam.system.vo;

import java.util.List;

public record QuestionImportResultVO(int successCount, int failCount, List<QuestionImportErrorVO> errors) {
}

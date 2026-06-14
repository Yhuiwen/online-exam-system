package com.exam.system.util;

import com.exam.system.constant.QuestionSourceCategory;
import com.exam.system.entity.Question;
import com.exam.system.exception.BusinessException;

import java.util.Set;

public final class QuestionSourceValidator {
    private static final Set<String> EXAM_SCOPES = Set.of("NATIONAL", "PROVINCIAL");

    private QuestionSourceValidator() {
    }

    public static void validateAndNormalize(Question question) {
        String category = QuestionSourceCategory.normalize(question.getSourceCategory());
        if (!QuestionSourceCategory.isValid(category)) {
            throw new BusinessException("题目分类不合法，可选：真题、模拟题、自命题、练习题");
        }
        switch (category) {
            case QuestionSourceCategory.PRACTICE -> clearArchiveFields(question);
            case QuestionSourceCategory.REAL_EXAM -> validateRealExam(question);
            case QuestionSourceCategory.MOCK_EXAM -> validateMockExam(question);
            case QuestionSourceCategory.SELF_AUTHORED -> validateSelfAuthored(question);
            default -> throw new BusinessException("题目分类不合法");
        }
        question.setSourceCategory(QuestionSourceCategory.storedValue(category));
    }

    public static String buildSummary(Question question) {
        String category = QuestionSourceCategory.normalize(question.getSourceCategory());
        if (QuestionSourceCategory.PRACTICE.equals(category)) {
            return "";
        }
        if (QuestionSourceCategory.REAL_EXAM.equals(category)) {
            StringBuilder builder = new StringBuilder();
            if (question.getExamYear() != null) {
                builder.append(question.getExamYear()).append("年");
            }
            if (hasText(question.getExamScope())) {
                if (!builder.isEmpty()) builder.append(' ');
                builder.append(scopeLabel(question.getExamScope()));
            }
            if (hasText(question.getProvince())) {
                if (!builder.isEmpty()) builder.append(' ');
                builder.append(question.getProvince().trim());
            }
            if (hasText(question.getPaperType())) {
                if (!builder.isEmpty()) builder.append(' ');
                builder.append(question.getPaperType().trim());
            }
            if (hasText(question.getSourceRef())) {
                if (!builder.isEmpty()) builder.append(" · ");
                builder.append(question.getSourceRef().trim());
            }
            return builder.toString();
        }
        return hasText(question.getSourceRef()) ? question.getSourceRef().trim() : "";
    }

    private static void validateRealExam(Question question) {
        if (question.getExamYear() == null) {
            throw new BusinessException("真题必须填写年份");
        }
        if (question.getExamYear() < 2000 || question.getExamYear() > 2100) {
            throw new BusinessException("年份应在 2000-2100 之间");
        }
        String scope = normalizeScope(question.getExamScope());
        if (!hasText(scope) || !EXAM_SCOPES.contains(scope)) {
            throw new BusinessException("真题必须选择考试类型（国考/省考）");
        }
        question.setExamScope(scope);
        if ("PROVINCIAL".equals(scope) && !hasText(question.getProvince())) {
            throw new BusinessException("省考真题必须填写省份");
        }
        if ("NATIONAL".equals(scope) && !hasText(question.getProvince())) {
            question.setProvince("全国");
        }
        if (!hasText(question.getSourceRef())) {
            throw new BusinessException("真题必须填写来源说明");
        }
        trimTextFields(question);
    }

    private static void validateMockExam(Question question) {
        if (!hasText(question.getSourceRef())) {
            throw new BusinessException("模拟题必须填写来源说明");
        }
        if (hasText(question.getExamScope())) {
            String scope = normalizeScope(question.getExamScope());
            if (!EXAM_SCOPES.contains(scope)) {
                throw new BusinessException("考试类型只能是国考或省考");
            }
            question.setExamScope(scope);
        }
        trimTextFields(question);
    }

    private static void validateSelfAuthored(Question question) {
        if (!hasText(question.getSourceRef())) {
            throw new BusinessException("自命题必须填写来源说明");
        }
        clearExamArchiveFields(question);
        trimTextFields(question);
    }

    private static void clearArchiveFields(Question question) {
        question.setExamYear(null);
        question.setExamScope(null);
        question.setProvince(null);
        question.setPaperType(null);
        question.setSourceRef(null);
    }

    private static void clearExamArchiveFields(Question question) {
        question.setExamYear(null);
        question.setExamScope(null);
        question.setProvince(null);
        question.setPaperType(null);
    }

    private static void trimTextFields(Question question) {
        question.setProvince(trimToNull(question.getProvince()));
        question.setPaperType(trimToNull(question.getPaperType()));
        question.setSourceRef(trimToNull(question.getSourceRef()));
    }

    private static String normalizeScope(String scope) {
        if (!hasText(scope)) return null;
        return switch (scope.trim()) {
            case "国考" -> "NATIONAL";
            case "省考" -> "PROVINCIAL";
            default -> scope.trim().toUpperCase();
        };
    }

    private static String scopeLabel(String scope) {
        if ("NATIONAL".equals(scope)) return "国考";
        if ("PROVINCIAL".equals(scope)) return "省考";
        return scope;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }
}

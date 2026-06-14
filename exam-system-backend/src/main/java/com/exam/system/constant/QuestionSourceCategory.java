package com.exam.system.constant;

import java.util.Locale;
import java.util.Set;

public final class QuestionSourceCategory {
    public static final String REAL_EXAM = "REAL_EXAM";
    public static final String MOCK_EXAM = "MOCK_EXAM";
    public static final String SELF_AUTHORED = "SELF_AUTHORED";
    public static final String PRACTICE = "PRACTICE";

    private static final Set<String> VALUES = Set.of(REAL_EXAM, MOCK_EXAM, SELF_AUTHORED, PRACTICE);

    private QuestionSourceCategory() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return PRACTICE;
        }
        String trimmed = value.trim();
        return switch (trimmed) {
            case "真题" -> REAL_EXAM;
            case "模拟题" -> MOCK_EXAM;
            case "自命题" -> SELF_AUTHORED;
            case "练习题" -> PRACTICE;
            default -> trimmed.toUpperCase(Locale.ROOT);
        };
    }

    public static boolean isValid(String value) {
        return VALUES.contains(normalize(value));
    }

    public static String label(String value) {
        return switch (normalize(value)) {
            case REAL_EXAM -> "真题";
            case MOCK_EXAM -> "模拟题";
            case SELF_AUTHORED -> "自命题";
            default -> "练习题";
        };
    }

    public static String storedValue(String value) {
        String normalized = normalize(value);
        return PRACTICE.equals(normalized) ? null : normalized;
    }
}

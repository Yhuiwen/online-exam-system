package com.exam.system.util;

import java.util.Map;

public final class ViolationRiskUtils {
    private static final Map<String, Integer> WEIGHTS = Map.of(
            "PAGE_HIDDEN", 5,
            "WINDOW_BLUR", 3,
            "FULLSCREEN_EXIT", 8,
            "COPY", 6,
            "PASTE", 10,
            "RIGHT_CLICK", 2,
            "DEVTOOLS_SUSPECTED", 15,
            "OTHER", 1
    );

    private ViolationRiskUtils() {
    }

    public static int weightOf(String violationType) {
        if (violationType == null) return WEIGHTS.get("OTHER");
        return WEIGHTS.getOrDefault(violationType.trim().toUpperCase(), WEIGHTS.get("OTHER"));
    }

    public static int scoreFromCounts(Map<String, Long> typeCounts) {
        if (typeCounts == null || typeCounts.isEmpty()) return 0;
        int score = 0;
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            long count = entry.getValue() == null ? 0 : entry.getValue();
            if (count > 0) {
                score += weightOf(entry.getKey()) * (int) count;
            }
        }
        return score;
    }

    public static String levelFromScore(int score) {
        if (score <= 0) return "NORMAL";
        if (score <= 10) return "LOW";
        if (score <= 30) return "MEDIUM";
        return "HIGH";
    }
}

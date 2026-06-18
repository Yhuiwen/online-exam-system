package com.exam.system.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViolationRiskUtilsTest {

    @Test
    void scoreFromCountsUsesWeightedSum() {
        int score = ViolationRiskUtils.scoreFromCounts(Map.of(
                "PAGE_HIDDEN", 2L,
                "COPY", 1L,
                "PASTE", 1L
        ));
        assertEquals(26, score);
    }

    @Test
    void levelFromScoreMapsBands() {
        assertEquals("NORMAL", ViolationRiskUtils.levelFromScore(0));
        assertEquals("LOW", ViolationRiskUtils.levelFromScore(8));
        assertEquals("MEDIUM", ViolationRiskUtils.levelFromScore(20));
        assertEquals("HIGH", ViolationRiskUtils.levelFromScore(45));
    }
}

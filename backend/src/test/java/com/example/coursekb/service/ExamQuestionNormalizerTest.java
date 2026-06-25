package com.example.coursekb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExamQuestionNormalizerTest {
    private final ExamQuestionNormalizer normalizer = new ExamQuestionNormalizer();

    @Test
    void normalizeQuestionNoKeepsRecognizablePaperNumbers() {
        assertEquals("1", normalizer.normalizeQuestionNo(" 第 1 题 "));
        assertEquals("2.1", normalizer.normalizeQuestionNo("Q2.1"));
        assertEquals("A3", normalizer.normalizeQuestionNo("A3"));
    }

    @Test
    void normalizeQuestionNoDropsUnreliableValues() {
        assertNull(normalizer.normalizeQuestionNo("??"));
        assertNull(normalizer.normalizeQuestionNo("第一题"));
    }

    @Test
    void normalizeQuestionTypeMapsKnownAliasesToDisplayLabels() {
        assertEquals("单选题", normalizer.normalizeQuestionType("CHOOSE"));
        assertEquals("简答题", normalizer.normalizeQuestionType("SHORT_ANSWER"));
        assertEquals("名词解释", normalizer.normalizeQuestionType("DEFINITION"));
        assertEquals("其他", normalizer.normalizeQuestionType("UNKNOWN_KIND"));
    }
}

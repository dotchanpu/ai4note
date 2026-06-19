package com.example.coursekb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {
    private final TextChunker textChunker = new TextChunker();

    @Test
    void keepsShortPageAsSingleChunk() {
        List<String> chunks = textChunker.split("第一段内容。\n第二段内容。");

        assertEquals(1, chunks.size());
        assertEquals("第一段内容。\n第二段内容。", chunks.get(0));
    }

    @Test
    void splitsLongTextAtSemanticBoundariesWithOverlap() {
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < 80; index++) {
            content.append("第")
                    .append(index)
                    .append("段：编译程序会依次执行词法分析、语法分析和语义分析。")
                    .append('\n');
        }

        List<String> chunks = textChunker.split(content.toString());

        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= TextChunker.MAX_CHUNK_LENGTH);
            assertFalse(chunk.trim().isEmpty());
        }

        assertTrue(hasSharedLine(chunks.get(0), chunks.get(1)));
    }

    @Test
    void hardSplitsSingleOversizedUnitWithinLimit() {
        String content = repeat("数据", 900);

        List<String> chunks = textChunker.split(content);

        assertEquals(2, chunks.size());
        assertEquals(TextChunker.MAX_CHUNK_LENGTH, chunks.get(0).length());
        assertTrue(chunks.get(1).length() <= TextChunker.MAX_CHUNK_LENGTH);
    }

    private boolean hasSharedLine(String first, String second) {
        String[] sentences = first.split("(?<=[。！？；.!?;])\\s*");
        for (String sentence : sentences) {
            if (!sentence.isEmpty() && second.contains(sentence)) {
                return true;
            }
        }
        return false;
    }

    private String repeat(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            result.append(value);
        }
        return result.toString();
    }
}

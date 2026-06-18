package com.example.coursekb.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {
    static final int MAX_CHUNK_LENGTH = 1200;
    static final int OVERLAP_LENGTH = 120;

    public List<String> split(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = content.trim();
        if (normalized.length() <= MAX_CHUNK_LENGTH) {
            return Collections.singletonList(normalized);
        }

        List<String> units = splitIntoUnits(normalized);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String unit : units) {
            if (unit.isEmpty()) {
                continue;
            }
            if (wouldExceedLimit(current, unit)) {
                String completed = current.toString().trim();
                if (!completed.isEmpty()) {
                    chunks.add(completed);
                }
                current.setLength(0);

                String overlap = trailingContext(completed);
                if (!overlap.isEmpty() && overlap.length() + 1 + unit.length() <= MAX_CHUNK_LENGTH) {
                    current.append(overlap);
                }
            }
            appendUnit(current, unit);
        }

        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            chunks.add(remaining);
        }
        return chunks;
    }

    private List<String> splitIntoUnits(String content) {
        List<String> units = new ArrayList<>();
        String[] semanticUnits = content.split("(?<=[。！？；.!?;])\\s*|\\n+");
        for (String semanticUnit : semanticUnits) {
            String unit = semanticUnit.trim();
            if (unit.isEmpty()) {
                continue;
            }
            if (unit.length() <= MAX_CHUNK_LENGTH) {
                units.add(unit);
            } else {
                units.addAll(splitOversizedUnit(unit));
            }
        }
        return units;
    }

    private List<String> splitOversizedUnit(String unit) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < unit.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, unit.length());
            if (end < unit.length()) {
                int preferredBreak = findPreferredBreak(unit, start, end);
                if (preferredBreak > start) {
                    end = preferredBreak;
                }
            }

            String part = unit.substring(start, end).trim();
            if (!part.isEmpty()) {
                parts.add(part);
            }
            start = end;
            while (start < unit.length() && Character.isWhitespace(unit.charAt(start))) {
                start++;
            }
        }
        return parts;
    }

    private int findPreferredBreak(String text, int start, int end) {
        int searchFloor = start + MAX_CHUNK_LENGTH / 2;
        for (int index = end; index > searchFloor; index--) {
            char character = text.charAt(index - 1);
            if (Character.isWhitespace(character)
                    || character == '，'
                    || character == ','
                    || character == '、'
                    || character == ':'
                    || character == '：') {
                return index;
            }
        }
        return end;
    }

    private boolean wouldExceedLimit(StringBuilder current, String unit) {
        int separatorLength = current.length() == 0 ? 0 : 1;
        return current.length() + separatorLength + unit.length() > MAX_CHUNK_LENGTH;
    }

    private void appendUnit(StringBuilder current, String unit) {
        if (current.length() > 0) {
            current.append('\n');
        }
        current.append(unit);
    }

    private String trailingContext(String content) {
        if (content.length() <= OVERLAP_LENGTH) {
            return content;
        }

        int start = content.length() - OVERLAP_LENGTH;
        for (int index = start; index < content.length(); index++) {
            char character = content.charAt(index);
            if (Character.isWhitespace(character)
                    || character == '。'
                    || character == '！'
                    || character == '？'
                    || character == '；'
                    || character == '.'
                    || character == '!'
                    || character == '?'
                    || character == ';') {
                return content.substring(index + 1).trim();
            }
        }
        return content.substring(start).trim();
    }
}

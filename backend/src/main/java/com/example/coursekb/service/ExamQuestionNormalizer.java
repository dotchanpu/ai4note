package com.example.coursekb.service;

import com.example.coursekb.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 负责把 AI 输出和查询侧使用的真题字段规整为稳定的内部值。
 */
@Component
public class ExamQuestionNormalizer {
    private static final Set<String> MATCH_SOURCES =
            new LinkedHashSet<String>(java.util.Arrays.asList("AI", "MANUAL"));
    private static final Pattern QUESTION_NO_PATTERN =
            Pattern.compile("^[A-Z]?\\d+(?:[.-]\\d+)*(?:[A-Z])?$");
    private static final Pattern LEADING_QUESTION_LABEL_PATTERN =
            Pattern.compile("^(?:QUESTION|Q|NO|NO\\.|题号|题目|第)+[:：.、\\-]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_QUESTION_LABEL_PATTERN =
            Pattern.compile("(?:题)?[.。:：、)）]+$");

    public String normalizeMatchSource(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "MANUAL"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!MATCH_SOURCES.contains(normalized)) {
            throw new BusinessException("Unsupported matchSource: " + normalized);
        }
        return normalized;
    }

    public String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    public String normalizeUpperText(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    /**
     * 将 AI 给出的题型收敛到一组稳定的前端展示值。
     */
    public String normalizeQuestionType(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        switch (normalizeUpperText(normalized)) {
            case "单选":
            case "单选题":
            case "CHOICE":
            case "CHOOSE":
            case "SINGLE_CHOICE":
            case "SINGLECHOICE":
                return "单选题";
            case "多选":
            case "多选题":
            case "MULTIPLE_CHOICE":
            case "MULTIPLECHOICE":
            case "MULTI_CHOICE":
                return "多选题";
            case "名词解释":
            case "DEFINITION":
            case "TERM_DEFINITION":
                return "简答题";
            case "填空":
            case "填空题":
            case "FILL_BLANK":
            case "FILL_IN_THE_BLANK":
            case "FILL":
                return "填空题";
            case "简答":
            case "简答题":
            case "SHORT_ANSWER":
            case "QUESTION_ANSWER":
            case "ESSAY":
            case "COMPARISON":
                return "简答题";
            case "编程":
            case "编程题":
            case "PROGRAMMING":
            case "CODE":
                return "编程题";
            case "设计":
            case "设计题":
            case "DESIGN":
            case "DESIGN_QUESTION":
                return "设计题";
            default:
                return "其他";
        }
    }

    /**
     * 能识别时尽量保留原卷面题号；无法可靠识别时返回 null，而不是伪造一个稳定编号。
     */
    public String normalizeQuestionNo(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replace('\u3000', ' ');
        normalized = normalized.replaceAll("\\s+", "");
        Matcher chineseNumberMatcher = Pattern.compile("^第?(\\d+(?:[.-]\\d+)*)题?$").matcher(normalized);
        if (chineseNumberMatcher.matches()) {
            return chineseNumberMatcher.group(1).toUpperCase(Locale.ROOT);
        }
        normalized = LEADING_QUESTION_LABEL_PATTERN.matcher(normalized).replaceFirst("");
        normalized = TRAILING_QUESTION_LABEL_PATTERN.matcher(normalized).replaceFirst("");
        if (normalized.startsWith("第") && normalized.endsWith("题") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!QUESTION_NO_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    public String requiredQuestionText(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? null : normalized;
    }

    public BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        try {
            return new BigDecimal(node.asText().trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public Integer integerOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        try {
            return Integer.valueOf(node.asText().trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public Long longOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isLong() || node.isInt()) {
            return node.asLong();
        }
        try {
            return Long.valueOf(node.asText().trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }
}

package com.example.coursekb.service;

import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 负责基于 AI 从已解析的资料文本中抽取真题。
 */
@Service
public class ExamExtractionAiService {
    private static final Logger log = LoggerFactory.getLogger(ExamExtractionAiService.class);
    private static final int MAX_EXTRACTION_BATCH_CHARS = 100000;
    private static final Pattern QUESTION_PAGE_SIGNAL = Pattern.compile(
            "(?is)(?:^|\\n)\\s*(?:[一二三四五六七八九十]+、.*(?:选择题|名词解释|简答题|编程题|设计题)|\\d+[.．、)].{0,120})");

    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;
    private final ExamQuestionNormalizer examQuestionNormalizer;

    public ExamExtractionAiService(
            DeepSeekService deepSeekService,
            ObjectMapper objectMapper,
            ExamQuestionNormalizer examQuestionNormalizer) {
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
        this.examQuestionNormalizer = examQuestionNormalizer;
    }

    public List<ExtractedQuestionPayload> extractWithAi(Material material, Long userId, List<TextChunk> chunks) {
        List<ExtractedQuestionPayload> result = new ArrayList<>();
        for (List<TextChunk> batch : buildExtractionBatches(chunks)) {
            result.addAll(extractBatchWithFallback(material, userId, batch));
        }
        return result;
    }

    private String buildSystemPrompt() {
        return "You are AI4Note's exam extraction assistant. "
                + "Read the source exam material text and split it into standalone exam questions. "
                + "Return a strict JSON object with the shape {\"questions\":[...]}. "
                + "Each question may only contain questionNo, questionType, questionText, answerText, difficultyLevel, score, examYear, sourcePage. "
                + "questionText is required and must not be empty. "
                + "questionText must include the complete question stem AND all answer choices (A/B/C/D) exactly as printed. "
                + "answerText must only be filled when the source material explicitly contains an answer or solution. "
                + "Never invent or guess answers. If the source does not show an answer, set answerText to null. "
                + "Use null for unknown fields. Do not output markdown or explanations.";
    }

    private String buildUserPrompt(Material material, List<TextChunk> chunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("Material title: ").append(material.getTitle()).append('\n');
        builder.append("Material type: ").append(material.getMaterialType()).append('\n');
        if (material.getYear() != null) {
            builder.append("Material year: ").append(material.getYear()).append('\n');
        }
        builder.append("Split only the following parsed pages into standalone exam questions. ");
        builder.append("Do not invent questions that are not present in these pages. ");
        builder.append("If a question continues across pages, use the visible text only.\n");
        for (TextChunk chunk : chunks) {
            builder.append("==== PAGE ")
                    .append(chunk.getPageNo() == null ? "?" : chunk.getPageNo())
                    .append(" ====\n")
                    .append(chunk.getContent())
                    .append("\n");
        }
        return builder.toString();
    }

    private List<List<TextChunk>> buildExtractionBatches(List<TextChunk> chunks) {
        if (chunks.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<TextChunk>> batches = new ArrayList<>();
        List<TextChunk> current = new ArrayList<>();
        int currentChars = 0;
        for (TextChunk chunk : chunks) {
            int chunkChars = chunk.getContent() == null ? 0 : chunk.getContent().length();
            if (!current.isEmpty() && currentChars + chunkChars > MAX_EXTRACTION_BATCH_CHARS) {
                batches.add(new ArrayList<>(current));
                current.clear();
                currentChars = 0;
            }
            current.add(chunk);
            currentChars += chunkChars;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    /**
     * 大批次返回坏 JSON，或者只覆盖了前半段页面时，递归拆分批次，直到结果足够完整或以明确错误失败。
     */
    private List<ExtractedQuestionPayload> extractBatchWithFallback(
            Material material, Long userId, List<TextChunk> batch) {
        String json = deepSeekService.generateJson(
                userId,
                material.getCourseId(),
                buildSystemPrompt(),
                buildUserPrompt(material, batch),
                null,
                262144);
        try {
            List<ExtractedQuestionPayload> extracted = parseExtractionPayloads(json);
            if (shouldSplitBatchForCoverage(batch, extracted)) {
                return splitBatchAndExtract(material, userId, batch);
            }
            return extracted;
        } catch (JsonProcessingException exception) {
            if (batch.size() > 1) {
                return splitBatchAndExtract(material, userId, batch);
            }
            log.warn(
                    "Failed to parse exam extraction JSON for materialId={}, pageNo={}, raw={}",
                    material.getId(),
                    batch.get(0).getPageNo(),
                    abbreviateForLog(json),
                    exception);
            throw new BusinessException("AI exam extraction JSON could not be parsed");
        }
    }

    private List<ExtractedQuestionPayload> parseExtractionPayloads(String json)
            throws JsonProcessingException {
        JsonNode root = parseAiJson(json);
        JsonNode questions = root.isArray() ? root : root.path("questions");
        if (!questions.isArray()) {
            return Collections.emptyList();
        }
        List<ExtractedQuestionPayload> result = new ArrayList<>();
        for (JsonNode node : questions) {
            String questionText = examQuestionNormalizer.requiredQuestionText(node.path("questionText").asText(null));
            if (questionText == null) {
                continue;
            }
            ExtractedQuestionPayload payload = new ExtractedQuestionPayload();
            payload.questionNo = examQuestionNormalizer.normalizeQuestionNo(node.path("questionNo").asText(null));
            payload.questionType = examQuestionNormalizer.normalizeQuestionType(node.path("questionType").asText(null));
            payload.questionText = questionText;
            payload.answerText = examQuestionNormalizer.normalizeOptionalText(node.path("answerText").asText(null));
            payload.difficultyLevel = examQuestionNormalizer.normalizeUpperText(node.path("difficultyLevel").asText(null));
            payload.score = examQuestionNormalizer.decimalOrNull(node.path("score"));
            payload.examYear = examQuestionNormalizer.integerOrNull(node.path("examYear"));
            payload.sourcePage = examQuestionNormalizer.integerOrNull(node.path("sourcePage"));
            result.add(payload);
        }
        return result;
    }

    /**
     * 如果批次中后半段页面看起来仍然有题目，但 AI 返回结果没有覆盖这些页面，就改走拆批重抽，
     * 避免把“只抽到前半卷”的半成品结果直接入库。
     */
    private boolean shouldSplitBatchForCoverage(
            List<TextChunk> batch, List<ExtractedQuestionPayload> extracted) {
        if (batch.size() <= 1 || extracted.isEmpty()) {
            return false;
        }
        Set<Integer> expectedPages = batch.stream()
                .filter(chunk -> chunk.getPageNo() != null)
                .filter(chunk -> looksLikeQuestionPage(chunk.getContent()))
                .map(TextChunk::getPageNo)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (expectedPages.isEmpty()) {
            return false;
        }
        Set<Integer> extractedPages = extracted.stream()
                .map(ExtractedQuestionPayload::getSourcePage)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (extractedPages.isEmpty()) {
            return false;
        }
        for (Integer expectedPage : expectedPages) {
            if (!extractedPages.contains(expectedPage)) {
                log.warn(
                        "Exam extraction coverage looks incomplete for pages={}, extractedPages={}",
                        expectedPages,
                        extractedPages);
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeQuestionPage(String content) {
        return content != null && QUESTION_PAGE_SIGNAL.matcher(content).find();
    }

    private List<ExtractedQuestionPayload> splitBatchAndExtract(
            Material material, Long userId, List<TextChunk> batch) {
        if (batch.size() <= 1) {
            return Collections.emptyList();
        }
        int midpoint = batch.size() / 2;
        List<ExtractedQuestionPayload> result = new ArrayList<>();
        result.addAll(extractBatchWithFallback(material, userId, new ArrayList<>(batch.subList(0, midpoint))));
        result.addAll(extractBatchWithFallback(material, userId, new ArrayList<>(batch.subList(midpoint, batch.size()))));
        return result;
    }

    public JsonNode parseAiJson(String raw) throws JsonProcessingException {
        String normalized = normalizeAiJson(raw);
        try {
            return objectMapper.readTree(normalized);
        } catch (JsonProcessingException ignored) {
            String extracted = extractJsonBody(normalized);
            if (extracted != null) {
                return objectMapper.readTree(extracted);
            }
            throw ignored;
        }
    }

    public String abbreviateForLog(String raw) {
        if (raw == null) {
            return "<null>";
        }
        String normalized = raw.replace("\r", "\\r").replace("\n", "\\n");
        return normalized.length() <= 1600 ? normalized : normalized.substring(0, 1600) + "...<truncated>";
    }

    private String normalizeAiJson(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd >= 0) {
                trimmed = trimmed.substring(firstLineEnd + 1).trim();
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private String extractJsonBody(String raw) {
        int objectStart = raw.indexOf('{');
        int arrayStart = raw.indexOf('[');
        if (objectStart < 0 && arrayStart < 0) {
            return null;
        }
        boolean useArray = objectStart < 0 || (arrayStart >= 0 && arrayStart < objectStart);
        int start = useArray ? arrayStart : objectStart;
        char opening = useArray ? '[' : '{';
        char closing = useArray ? ']' : '}';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == opening) {
                depth++;
            } else if (current == closing) {
                depth--;
                if (depth == 0) {
                    return raw.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    public static class ExtractedQuestionPayload {
        private String questionNo;
        private String questionType;
        private String questionText;
        private String answerText;
        private String difficultyLevel;
        private BigDecimal score;
        private Integer examYear;
        private Integer sourcePage;

        public String getQuestionNo() {
            return questionNo;
        }

        public String getQuestionType() {
            return questionType;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getAnswerText() {
            return answerText;
        }

        public String getDifficultyLevel() {
            return difficultyLevel;
        }

        public BigDecimal getScore() {
            return score;
        }

        public Integer getExamYear() {
            return examYear;
        }

        public Integer getSourcePage() {
            return sourcePage;
        }
    }
}

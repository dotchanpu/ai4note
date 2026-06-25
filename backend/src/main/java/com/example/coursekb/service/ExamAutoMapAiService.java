package com.example.coursekb.service;

import com.example.coursekb.entity.Chapter;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.ExamQuestionKnowledgeMapRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 负责将抽出的真题通过 AI 自动映射到当前课程已有知识条目。
 */
@Service
public class ExamAutoMapAiService {
    private static final Logger log = LoggerFactory.getLogger(ExamAutoMapAiService.class);
    private static final int MAX_AUTO_MAP_BATCH_QUESTIONS = 12;
    private static final int MAX_AUTO_MAP_KNOWLEDGE_ITEMS = 80;
    private static final BigDecimal MIN_AUTO_MAP_CONFIDENCE = new BigDecimal("60");

    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;
    private final ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final ChapterRepository chapterRepository;
    private final ExamQuestionNormalizer examQuestionNormalizer;

    public ExamAutoMapAiService(
            DeepSeekService deepSeekService,
            ObjectMapper objectMapper,
            ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            ChapterRepository chapterRepository,
            ExamQuestionNormalizer examQuestionNormalizer) {
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
        this.examQuestionKnowledgeMapRepository = examQuestionKnowledgeMapRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.chapterRepository = chapterRepository;
        this.examQuestionNormalizer = examQuestionNormalizer;
    }

    public void autoMapQuestions(Material material, Long userId, List<ExamQuestion> questions) {
        if (questions.isEmpty()) {
            return;
        }
        List<KnowledgeItem> knowledgeItems = knowledgeItemRepository
                .findByCourseIdOrderByImportanceLevelDescIdDesc(material.getCourseId())
                .stream()
                .limit(MAX_AUTO_MAP_KNOWLEDGE_ITEMS)
                .collect(Collectors.toList());
        if (knowledgeItems.isEmpty()) {
            return;
        }
        Map<Long, KnowledgeItem> knowledgeById = knowledgeItems.stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, Function.identity()));
        Map<Long, String> chapterTitleById = chapterRepository.findAllById(
                        knowledgeItems.stream()
                                .map(KnowledgeItem::getChapterId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getChapterTitle));
        for (List<ExamQuestion> batch : buildQuestionBatches(questions)) {
            autoMapQuestionBatch(material, userId, batch, knowledgeItems, knowledgeById, chapterTitleById);
        }
    }

    private String buildAutoMapSystemPrompt() {
        return "You are AI4Note's exam-to-knowledge mapping assistant. "
                + "Map each exam question to the most relevant existing course knowledge items. "
                + "Return a strict JSON object with the shape {\"mappings\":[...]}. "
                + "Each mapping item may only contain questionId, knowledgeItemId, confidenceScore, reason. "
                + "Use only the provided question ids and knowledge item ids. "
                + "Prefer precise mappings and omit weak matches. "
                + "Do not output markdown or explanations.";
    }

    private String buildAutoMapUserPrompt(
            Material material,
            List<ExamQuestion> questions,
            List<KnowledgeItem> knowledgeItems,
            Map<Long, String> chapterTitleById) {
        StringBuilder builder = new StringBuilder();
        builder.append("Course id: ").append(material.getCourseId()).append('\n');
        builder.append("Material title: ").append(material.getTitle()).append('\n');
        builder.append("Questions to map:\n");
        for (ExamQuestion question : questions) {
            builder.append("- questionId: ").append(question.getId()).append('\n');
            if (question.getQuestionNo() != null) {
                builder.append("  questionNo: ").append(question.getQuestionNo()).append('\n');
            }
            if (question.getQuestionType() != null) {
                builder.append("  questionType: ").append(question.getQuestionType()).append('\n');
            }
            if (question.getSourcePage() != null) {
                builder.append("  sourcePage: ").append(question.getSourcePage()).append('\n');
            }
            builder.append("  questionText: ").append(question.getQuestionText()).append('\n');
            if (question.getAnswerText() != null) {
                builder.append("  answerText: ").append(question.getAnswerText()).append('\n');
            }
        }
        builder.append("Candidate knowledge items:\n");
        for (KnowledgeItem item : knowledgeItems) {
            builder.append("- knowledgeItemId: ").append(item.getId()).append('\n');
            builder.append("  title: ").append(item.getTitle()).append('\n');
            builder.append("  itemType: ").append(item.getItemType()).append('\n');
            if (item.getChapterId() != null && chapterTitleById.containsKey(item.getChapterId())) {
                builder.append("  chapterTitle: ").append(chapterTitleById.get(item.getChapterId())).append('\n');
            }
            builder.append("  content: ").append(item.getContent()).append('\n');
        }
        builder.append("Rules:\n");
        builder.append("1. Each question may map to up to 3 knowledge items.\n");
        builder.append("2. If no candidate is suitable, omit that question.\n");
        builder.append("3. confidenceScore must be between 0 and 100.\n");
        builder.append("4. reason should be brief and concrete.\n");
        return builder.toString();
    }

    private List<List<ExamQuestion>> buildQuestionBatches(List<ExamQuestion> questions) {
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<ExamQuestion>> batches = new ArrayList<>();
        for (int start = 0; start < questions.size(); start += MAX_AUTO_MAP_BATCH_QUESTIONS) {
            int end = Math.min(start + MAX_AUTO_MAP_BATCH_QUESTIONS, questions.size());
            batches.add(new ArrayList<>(questions.subList(start, end)));
        }
        return batches;
    }

    /**
     * 低置信度映射在这里直接丢弃，避免后续统计建立在不可靠结果上。
     */
    private void autoMapQuestionBatch(
            Material material,
            Long userId,
            List<ExamQuestion> questions,
            List<KnowledgeItem> knowledgeItems,
            Map<Long, KnowledgeItem> knowledgeById,
            Map<Long, String> chapterTitleById) {
        String json = deepSeekService.generateJson(
                userId,
                material.getCourseId(),
                buildAutoMapSystemPrompt(),
                buildAutoMapUserPrompt(material, questions, knowledgeItems, chapterTitleById),
                null,
                3072);
        Set<Long> questionIds = questions.stream().map(ExamQuestion::getId).collect(Collectors.toSet());
        try {
            JsonNode root = parseAiJson(json);
            JsonNode mappings = root.isArray() ? root : root.path("mappings");
            if (!mappings.isArray()) {
                return;
            }
            for (JsonNode node : mappings) {
                Long questionId = examQuestionNormalizer.longOrNull(node.path("questionId"));
                Long knowledgeItemId = examQuestionNormalizer.longOrNull(node.path("knowledgeItemId"));
                BigDecimal confidenceScore = examQuestionNormalizer.decimalOrNull(node.path("confidenceScore"));
                String reason = examQuestionNormalizer.normalizeOptionalText(node.path("reason").asText(null));
                if (questionId == null || knowledgeItemId == null) {
                    continue;
                }
                if (!questionIds.contains(questionId) || !knowledgeById.containsKey(knowledgeItemId)) {
                    continue;
                }
                if (confidenceScore == null || confidenceScore.compareTo(MIN_AUTO_MAP_CONFIDENCE) < 0) {
                    continue;
                }
                if (reason != null && reason.toLowerCase(Locale.ROOT).contains("no relevant")) {
                    continue;
                }
                ExamQuestionKnowledgeMap map = examQuestionKnowledgeMapRepository
                        .findByExamQuestionIdAndKnowledgeItemId(questionId, knowledgeItemId)
                        .orElseGet(ExamQuestionKnowledgeMap::new);
                map.setExamQuestionId(questionId);
                map.setKnowledgeItemId(knowledgeItemId);
                map.setMatchSource("AI");
                map.setConfidenceScore(confidenceScore);
                map.setReason(reason);
                examQuestionKnowledgeMapRepository.save(map);
            }
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Failed to parse auto-map JSON for materialId={}, raw={}",
                    material.getId(),
                    abbreviateForLog(json),
                    exception);
        }
    }

    private JsonNode parseAiJson(String raw) throws JsonProcessingException {
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

    private String abbreviateForLog(String raw) {
        if (raw == null) {
            return "<null>";
        }
        String normalized = raw.replace("\r", "\\r").replace("\n", "\\n");
        return normalized.length() <= 1600 ? normalized : normalized.substring(0, 1600) + "...<truncated>";
    }
}

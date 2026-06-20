package com.example.coursekb.service;

import com.example.coursekb.dto.ExamKnowledgeMapRequest;
import com.example.coursekb.entity.Chapter;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.MaterialFile;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.ExamQuestionKnowledgeMapRepository;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExamQuestionPageVO;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamQuestionService {
    private static final Logger log = LoggerFactory.getLogger(ExamQuestionService.class);
    private static final Set<String> MATCH_SOURCES =
            new LinkedHashSet<String>(java.util.Arrays.asList("AI", "MANUAL"));
    private static final int MAX_EXTRACTION_BATCH_CHARS = 2200;
    private static final int MAX_AUTO_MAP_BATCH_QUESTIONS = 12;
    private static final int MAX_AUTO_MAP_KNOWLEDGE_ITEMS = 80;
    private static final BigDecimal MIN_AUTO_MAP_CONFIDENCE = new BigDecimal("60");
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String CODE_NOT_PARSED = "MATERIAL_NOT_PARSED";
    private static final String CODE_ALREADY_EXTRACTED = "EXAM_EXTRACTION_EXISTS";
    private static final String CODE_PDF_ONLY = "EXAM_PDF_ONLY";
    private static final Pattern QUESTION_NO_PATTERN =
            Pattern.compile("^[A-Z]?\\d+(?:[.-]\\d+)*(?:[A-Z])?$");
    private static final Pattern LEADING_QUESTION_LABEL_PATTERN =
            Pattern.compile("^(?:QUESTION|Q|NO|NO\\.|题号|题目|第)+[:：.、\\-]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_QUESTION_LABEL_PATTERN =
            Pattern.compile("(?:题)?[.。:：、)）]+$");

    private final MaterialService materialService;
    private final CourseService courseService;
    private final TextChunkRepository textChunkRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final MaterialFileRepository materialFileRepository;
    private final MaterialRepository materialRepository;
    private final ChapterRepository chapterRepository;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;

    public ExamQuestionService(
            MaterialService materialService,
            CourseService courseService,
            TextChunkRepository textChunkRepository,
            ExamQuestionRepository examQuestionRepository,
            ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            MaterialFileRepository materialFileRepository,
            MaterialRepository materialRepository,
            ChapterRepository chapterRepository,
            DeepSeekService deepSeekService,
            ObjectMapper objectMapper) {
        this.materialService = materialService;
        this.courseService = courseService;
        this.textChunkRepository = textChunkRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.examQuestionKnowledgeMapRepository = examQuestionKnowledgeMapRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.materialFileRepository = materialFileRepository;
        this.materialRepository = materialRepository;
        this.chapterRepository = chapterRepository;
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<ExamQuestionVO> extractQuestions(Long materialId, Long userId, boolean overwrite) {
        Material material = materialService.getOwnedMaterial(materialId, userId);
        ensureExamMaterial(material);

        List<TextChunk> chunks = textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(materialId);
        if (chunks.isEmpty()) {
            throw new BusinessException(CODE_NOT_PARSED, "Material must be parsed before exam extraction");
        }

        long existingCount = examQuestionRepository.countByMaterialId(materialId);
        if (existingCount > 0 && !overwrite) {
            throw new BusinessException(CODE_ALREADY_EXTRACTED, "Exam questions already exist for this material");
        }

        List<ExtractedQuestionPayload> extracted = extractWithAi(material, userId, chunks);
        if (extracted.isEmpty()) {
            throw new BusinessException("No valid exam questions were extracted");
        }

        if (existingCount > 0) {
            replaceMaterialQuestions(materialId);
        }

        List<ExamQuestion> saved = examQuestionRepository.saveAll(extracted.stream()
                .map(payload -> toExamQuestion(material, payload))
                .collect(Collectors.toList()));
        autoMapQuestions(material, userId, saved);
        return toQuestionVOs(saved);
    }

    public ExamQuestionPageVO listQuestions(
            Long courseId,
            Long userId,
            Integer year,
            Long chapterId,
            String questionType,
            Long materialId,
            Integer page,
            Integer size) {
        courseService.getOwnedCourse(courseId, userId);
        List<ExamQuestion> questions = filterQuestions(courseId, year, chapterId, questionType, materialId);
        int safePage = normalizePage(page);
        int safeSize = normalizePageSize(size);
        long total = questions.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize);
        int fromIndex = Math.min((safePage - 1) * safeSize, questions.size());
        int toIndex = Math.min(fromIndex + safeSize, questions.size());
        return ExamQuestionPageVO.of(toQuestionVOs(questions.subList(fromIndex, toIndex)), total, safePage, safeSize, totalPages);
    }

    private List<ExamQuestion> filterQuestions(
            Long courseId, Integer year, Long chapterId, String questionType, Long materialId) {
        String normalizedQuestionType = normalizeQuestionType(questionType);
        return examQuestionRepository.findByCourseIdOrderByExamYearDescIdDesc(courseId)
                .stream()
                .filter(question -> year == null || Objects.equals(year, question.getExamYear()))
                .filter(question -> chapterId == null || Objects.equals(chapterId, question.getChapterId()))
                .filter(question -> materialId == null || Objects.equals(materialId, question.getMaterialId()))
                .filter(question -> normalizedQuestionType == null
                        || normalizedQuestionType.equals(normalizeQuestionType(question.getQuestionType())))
                .sorted(this::compareQuestions)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExamQuestionKnowledgeMapVO saveKnowledgeMap(
            Long questionId, Long userId, ExamKnowledgeMapRequest request) {
        ExamQuestion question = examQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("Exam question does not exist"));
        courseService.getOwnedCourse(question.getCourseId(), userId);

        KnowledgeItem knowledgeItem = knowledgeItemRepository.findByIdAndCourseId(
                        request.getKnowledgeItemId(), question.getCourseId())
                .orElseThrow(() -> new BusinessException("Knowledge item does not exist in this course"));

        ExamQuestionKnowledgeMap map = examQuestionKnowledgeMapRepository
                .findByExamQuestionIdAndKnowledgeItemId(questionId, knowledgeItem.getId())
                .orElseGet(ExamQuestionKnowledgeMap::new);
        map.setExamQuestionId(questionId);
        map.setKnowledgeItemId(knowledgeItem.getId());
        map.setMatchSource(normalizeMatchSource(request.getMatchSource()));
        map.setConfidenceScore(request.getConfidenceScore());
        map.setReason(normalizeOptionalText(request.getReason()));

        ExamQuestionKnowledgeMap saved = examQuestionKnowledgeMapRepository.save(map);
        return ExamQuestionKnowledgeMapVO.from(saved, knowledgeItem.getTitle(), knowledgeItem.getItemType());
    }

    public List<ExamKnowledgeStatVO> listKnowledgeStats(
            Long courseId,
            Long userId,
            Integer year,
            Long chapterId,
            String questionType) {
        courseService.getOwnedCourse(courseId, userId);
        List<ExamQuestion> filteredQuestions = filterQuestions(courseId, year, chapterId, questionType, null);
        if (filteredQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ExamQuestion> questionById = filteredQuestions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
        List<ExamQuestionKnowledgeMap> mappings = examQuestionKnowledgeMapRepository
                .findByExamQuestionIdInOrderByIdAsc(questionById.keySet());
        if (mappings.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, KnowledgeItem> knowledgeById = knowledgeItemRepository.findAllById(
                        mappings.stream()
                                .map(ExamQuestionKnowledgeMap::getKnowledgeItemId)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, Function.identity()));
        Map<Long, String> chapterTitleById = chapterRepository.findAllById(
                        knowledgeById.values().stream()
                                .map(KnowledgeItem::getChapterId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getChapterTitle));

        Map<Long, StatAccumulator> stats = new LinkedHashMap<>();
        for (ExamQuestionKnowledgeMap mapping : mappings) {
            ExamQuestion question = questionById.get(mapping.getExamQuestionId());
            KnowledgeItem knowledgeItem = knowledgeById.get(mapping.getKnowledgeItemId());
            if (question == null || knowledgeItem == null) {
                continue;
            }
            StatAccumulator accumulator = stats.computeIfAbsent(knowledgeItem.getId(), ignored -> {
                StatAccumulator created = new StatAccumulator();
                created.knowledgeItem = knowledgeItem;
                created.chapterTitle = knowledgeItem.getChapterId() == null
                        ? null
                        : chapterTitleById.get(knowledgeItem.getChapterId());
                created.totalScore = BigDecimal.ZERO;
                created.latestExamYear = question.getExamYear();
                return created;
            });
            accumulator.questionCount++;
            accumulator.totalScore = accumulator.totalScore.add(
                    question.getScore() == null ? BigDecimal.ZERO : question.getScore());
            accumulator.latestExamYear = maxYear(accumulator.latestExamYear, question.getExamYear());
        }

        return stats.values().stream()
                .map(accumulator -> {
                    ExamKnowledgeStatVO result = new ExamKnowledgeStatVO();
                    result.setKnowledgeItemId(accumulator.knowledgeItem.getId());
                    result.setKnowledgeTitle(accumulator.knowledgeItem.getTitle());
                    result.setKnowledgeItemType(accumulator.knowledgeItem.getItemType());
                    result.setChapterTitle(accumulator.chapterTitle);
                    result.setQuestionCount(accumulator.questionCount);
                    result.setTotalScore(accumulator.totalScore);
                    result.setLatestExamYear(accumulator.latestExamYear);
                    return result;
                })
                .sorted(Comparator.comparingLong(ExamKnowledgeStatVO::getQuestionCount)
                        .reversed()
                        .thenComparing(ExamKnowledgeStatVO::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExamKnowledgeStatVO::getKnowledgeItemId))
                .collect(Collectors.toList());
    }

    private void ensureExamMaterial(Material material) {
        if (!"EXAM".equalsIgnoreCase(material.getMaterialType())) {
            throw new BusinessException("Only EXAM materials support exam extraction");
        }
        MaterialFile file = materialFileRepository.findByMaterialId(material.getId())
                .orElseThrow(() -> new BusinessException("Material file does not exist"));
        if (!"pdf".equalsIgnoreCase(file.getFileType())) {
            throw new BusinessException(CODE_PDF_ONLY, "Only PDF exam materials support extraction");
        }
    }

    private List<ExtractedQuestionPayload> extractWithAi(
            Material material, Long userId, List<TextChunk> chunks) {
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
                + "Use null for unknown fields. Do not output markdown or explanations.";
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

    private void autoMapQuestions(Material material, Long userId, List<ExamQuestion> questions) {
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
            JsonNode root = parseAiExtractionJson(json);
            JsonNode mappings = root.isArray() ? root : root.path("mappings");
            if (!mappings.isArray()) {
                return;
            }
            for (JsonNode node : mappings) {
                Long questionId = longOrNull(node.path("questionId"));
                Long knowledgeItemId = longOrNull(node.path("knowledgeItemId"));
                BigDecimal confidenceScore = decimalOrNull(node.path("confidenceScore"));
                String reason = normalizeOptionalText(node.path("reason").asText(null));
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

    private List<ExtractedQuestionPayload> extractBatchWithFallback(
            Material material, Long userId, List<TextChunk> batch) {
        String json = deepSeekService.generateJson(
                userId,
                material.getCourseId(),
                buildSystemPrompt(),
                buildUserPrompt(material, batch),
                null,
                3072);
        try {
            return parseExtractionPayloads(json);
        } catch (JsonProcessingException exception) {
            if (batch.size() > 1) {
                int midpoint = batch.size() / 2;
                List<ExtractedQuestionPayload> result = new ArrayList<>();
                result.addAll(extractBatchWithFallback(material, userId, new ArrayList<>(batch.subList(0, midpoint))));
                result.addAll(extractBatchWithFallback(material, userId, new ArrayList<>(batch.subList(midpoint, batch.size()))));
                return result;
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
        JsonNode root = parseAiExtractionJson(json);
        JsonNode questions = root.isArray() ? root : root.path("questions");
        if (!questions.isArray()) {
            return Collections.emptyList();
        }
        List<ExtractedQuestionPayload> result = new ArrayList<>();
        for (JsonNode node : questions) {
            String questionText = requiredQuestionText(node.path("questionText").asText(null));
            if (questionText == null) {
                continue;
            }
            ExtractedQuestionPayload payload = new ExtractedQuestionPayload();
            payload.questionNo = normalizeQuestionNo(node.path("questionNo").asText(null));
            payload.questionType = normalizeQuestionType(node.path("questionType").asText(null));
            payload.questionText = questionText;
            payload.answerText = normalizeOptionalText(node.path("answerText").asText(null));
            payload.difficultyLevel = normalizeUpperText(node.path("difficultyLevel").asText(null));
            payload.score = decimalOrNull(node.path("score"));
            payload.examYear = integerOrNull(node.path("examYear"));
            payload.sourcePage = integerOrNull(node.path("sourcePage"));
            result.add(payload);
        }
        return result;
    }

    private JsonNode parseAiExtractionJson(String raw) throws JsonProcessingException {
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

    private void replaceMaterialQuestions(Long materialId) {
        List<Long> questionIds = examQuestionRepository.findByMaterialIdOrderByIdAsc(materialId)
                .stream()
                .map(ExamQuestion::getId)
                .collect(Collectors.toList());
        if (!questionIds.isEmpty()) {
            examQuestionKnowledgeMapRepository.deleteByExamQuestionIdIn(questionIds);
        }
        examQuestionRepository.deleteAll(
                examQuestionRepository.findByMaterialIdOrderByIdAsc(materialId));
    }

    private ExamQuestion toExamQuestion(Material material, ExtractedQuestionPayload payload) {
        ExamQuestion question = new ExamQuestion();
        question.setCourseId(material.getCourseId());
        question.setMaterialId(material.getId());
        question.setChapterId(material.getChapterId());
        question.setQuestionNo(normalizeQuestionNo(payload.questionNo));
        question.setQuestionType(normalizeQuestionType(payload.questionType));
        question.setQuestionText(payload.questionText);
        question.setAnswerText(payload.answerText);
        question.setDifficultyLevel(payload.difficultyLevel);
        question.setScore(payload.score);
        question.setExamYear(payload.examYear == null ? material.getYear() : payload.examYear);
        question.setSourcePage(payload.sourcePage);
        return question;
    }

    private List<ExamQuestionVO> toQuestionVOs(List<ExamQuestion> questions) {
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<Long> questionIds = questions.stream().map(ExamQuestion::getId).collect(Collectors.toList());
        List<ExamQuestionKnowledgeMap> mappings =
                examQuestionKnowledgeMapRepository.findByExamQuestionIdInOrderByIdAsc(questionIds);
        Map<Long, KnowledgeItem> knowledgeById = knowledgeItemRepository.findAllById(
                        mappings.stream()
                                .map(ExamQuestionKnowledgeMap::getKnowledgeItemId)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, Function.identity()));

        Map<Long, String> materialTitleById = materialRepository.findAllById(
                        questions.stream()
                                .map(ExamQuestion::getMaterialId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Material::getId, Material::getTitle));
        Map<Long, String> chapterTitleById = chapterRepository.findAllById(
                        questions.stream()
                                .map(ExamQuestion::getChapterId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getChapterTitle));

        Map<Long, List<ExamQuestionKnowledgeMapVO>> mappingsByQuestionId = mappings.stream()
                .collect(Collectors.groupingBy(
                        ExamQuestionKnowledgeMap::getExamQuestionId,
                        LinkedHashMap::new,
                        Collectors.mapping(map -> {
                            KnowledgeItem knowledgeItem = knowledgeById.get(map.getKnowledgeItemId());
                            return ExamQuestionKnowledgeMapVO.from(
                                    map,
                                    knowledgeItem == null ? null : knowledgeItem.getTitle(),
                                    knowledgeItem == null ? null : knowledgeItem.getItemType());
                        }, Collectors.toList())));

        return questions.stream()
                .map(question -> ExamQuestionVO.from(
                        question,
                        normalizeQuestionNo(question.getQuestionNo()),
                        normalizeQuestionType(question.getQuestionType()),
                        question.getMaterialId() == null ? null : materialTitleById.get(question.getMaterialId()),
                        question.getChapterId() == null ? null : chapterTitleById.get(question.getChapterId()),
                        mappingsByQuestionId.get(question.getId())))
                .collect(Collectors.toList());
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private int compareQuestions(ExamQuestion left, ExamQuestion right) {
        int byYear = compareNullableIntegersDesc(left.getExamYear(), right.getExamYear());
        if (byYear != 0) {
            return byYear;
        }
        int byMaterial = compareNullableLongsDesc(left.getMaterialId(), right.getMaterialId());
        if (byMaterial != 0) {
            return byMaterial;
        }
        int byPage = compareNullableIntegersAsc(left.getSourcePage(), right.getSourcePage());
        if (byPage != 0) {
            return byPage;
        }
        int byQuestionNo = compareQuestionNos(left.getQuestionNo(), right.getQuestionNo());
        if (byQuestionNo != 0) {
            return byQuestionNo;
        }
        return compareNullableLongsAsc(left.getId(), right.getId());
    }

    private int compareQuestionNos(String left, String right) {
        QuestionNoSortKey leftKey = QuestionNoSortKey.from(normalizeQuestionNo(left));
        QuestionNoSortKey rightKey = QuestionNoSortKey.from(normalizeQuestionNo(right));
        if (leftKey.parsed != rightKey.parsed) {
            return leftKey.parsed ? -1 : 1;
        }
        if (!leftKey.parsed) {
            return 0;
        }
        int prefixCompare = leftKey.prefix.compareTo(rightKey.prefix);
        if (prefixCompare != 0) {
            return prefixCompare;
        }
        int length = Math.max(leftKey.numbers.size(), rightKey.numbers.size());
        for (int index = 0; index < length; index++) {
            int leftNumber = index < leftKey.numbers.size() ? leftKey.numbers.get(index) : -1;
            int rightNumber = index < rightKey.numbers.size() ? rightKey.numbers.get(index) : -1;
            int compare = Integer.compare(leftNumber, rightNumber);
            if (compare != 0) {
                return compare;
            }
        }
        return leftKey.suffix.compareTo(rightKey.suffix);
    }

    private int compareNullableIntegersDesc(Integer left, Integer right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private int compareNullableIntegersAsc(Integer left, Integer right) {
        return Comparator.nullsLast(Comparator.<Integer>naturalOrder()).compare(left, right);
    }

    private int compareNullableLongsDesc(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private int compareNullableLongsAsc(Long left, Long right) {
        return Comparator.nullsLast(Comparator.<Long>naturalOrder()).compare(left, right);
    }

    private String normalizeMatchSource(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "MANUAL"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!MATCH_SOURCES.contains(normalized)) {
            throw new BusinessException("Unsupported matchSource: " + normalized);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeUpperText(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String normalizeQuestionType(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        switch (normalizeUpperText(normalized)) {
            case "单选":
            case "单选题":
            case "CHOICE":
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
                return "名词解释";
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

    private String normalizeQuestionNo(String value) {
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

    private String requiredQuestionText(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? null : normalized;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
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

    private Integer integerOrNull(JsonNode node) {
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

    private Long longOrNull(JsonNode node) {
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

    private Integer maxYear(Integer current, Integer candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return Math.max(current, candidate);
    }

    private static class ExtractedQuestionPayload {
        private String questionNo;
        private String questionType;
        private String questionText;
        private String answerText;
        private String difficultyLevel;
        private BigDecimal score;
        private Integer examYear;
        private Integer sourcePage;
    }

    private static class StatAccumulator {
        private KnowledgeItem knowledgeItem;
        private String chapterTitle;
        private long questionCount;
        private BigDecimal totalScore;
        private Integer latestExamYear;
    }

    private static class QuestionNoSortKey {
        private final boolean parsed;
        private final String prefix;
        private final List<Integer> numbers;
        private final String suffix;

        private QuestionNoSortKey(boolean parsed, String prefix, List<Integer> numbers, String suffix) {
            this.parsed = parsed;
            this.prefix = prefix;
            this.numbers = numbers;
            this.suffix = suffix;
        }

        private static QuestionNoSortKey from(String value) {
            String normalized = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
            if (normalized == null || normalized.isEmpty()) {
                return new QuestionNoSortKey(false, "", Collections.emptyList(), "");
            }
            Matcher matcher = Pattern.compile("^([A-Z]?)(\\d+(?:[.-]\\d+)*)([A-Z]?)$").matcher(normalized);
            if (!matcher.matches()) {
                return new QuestionNoSortKey(false, "", Collections.emptyList(), "");
            }
            List<Integer> numbers = new ArrayList<>();
            for (String part : matcher.group(2).split("[.-]")) {
                numbers.add(Integer.parseInt(part));
            }
            return new QuestionNoSortKey(true, matcher.group(1), numbers, matcher.group(3));
        }
    }
}

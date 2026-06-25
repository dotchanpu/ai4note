package com.example.coursekb.service;

import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.ExamQuestionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 负责为已有真题生成参考答案，并优先利用课程课件和笔记作为上下文。
 */
@Service
public class ExamQuestionAnswerService {
    private static final Logger log = LoggerFactory.getLogger(ExamQuestionAnswerService.class);
    private static final ExecutorService BATCH_ANSWER_EXECUTOR = Executors.newFixedThreadPool(10);

    private final CourseService courseService;
    private final ExamQuestionRepository examQuestionRepository;
    private final MaterialRepository materialRepository;
    private final TextChunkRepository textChunkRepository;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper;
    private final ExamQuestionAssembler examQuestionAssembler;
    private final ExamQuestionNormalizer examQuestionNormalizer;

    public ExamQuestionAnswerService(
            CourseService courseService,
            ExamQuestionRepository examQuestionRepository,
            MaterialRepository materialRepository,
            TextChunkRepository textChunkRepository,
            DeepSeekService deepSeekService,
            ObjectMapper objectMapper,
            ExamQuestionAssembler examQuestionAssembler,
            ExamQuestionNormalizer examQuestionNormalizer) {
        this.courseService = courseService;
        this.examQuestionRepository = examQuestionRepository;
        this.materialRepository = materialRepository;
        this.textChunkRepository = textChunkRepository;
        this.deepSeekService = deepSeekService;
        this.objectMapper = objectMapper;
        this.examQuestionAssembler = examQuestionAssembler;
        this.examQuestionNormalizer = examQuestionNormalizer;
    }

    @Transactional
    public ExamQuestionVO generateAnswer(Long questionId, Long userId) {
        ExamQuestion question = examQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("Exam question does not exist"));
        courseService.getOwnedCourse(question.getCourseId(), userId);

        List<Material> referenceMaterials = materialRepository
                .findByCourseIdAndMaterialTypeInOrderByUploadTimeDesc(question.getCourseId(), Arrays.asList("SLIDE", "NOTE"));
        if (referenceMaterials.isEmpty()) {
            throw new BusinessException("No SLIDE or NOTE materials found in this course for answer generation");
        }

        List<Material> sortedMaterials = new ArrayList<>(referenceMaterials);
        sortedMaterials.sort((a, b) -> {
            int typeCompare = Integer.compare(materialTypePriority(a.getMaterialType()), materialTypePriority(b.getMaterialType()));
            if (typeCompare != 0) {
                return typeCompare;
            }
            return Comparator.nullsFirst(Integer::compareTo).compare(a.getYear(), b.getYear());
        });

        List<Long> materialIds = sortedMaterials.stream().map(Material::getId).collect(Collectors.toList());
        List<TextChunk> allChunks = textChunkRepository.findByMaterialIdInOrderByPageNoAscChunkIndexAsc(materialIds);
        Map<Long, Material> materialById = sortedMaterials.stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));

        // 优先使用课件和笔记片段，这样生成的答案才能回指到明确的上传资料来源。
        List<TextChunk> relevantChunks = selectRelevantChunks(question.getQuestionText(), allChunks, materialById);

        String json = deepSeekService.generateJson(
                userId,
                question.getCourseId(),
                buildAnswerGenSystemPrompt(),
                buildAnswerGenUserPrompt(question, relevantChunks, materialById),
                null,
                2048);

        AnswerGenPayload payload = parseAnswerGenJson(json);
        question.setAnswerText(payload.answerText);
        question.setAnswerSource(payload.answerSource);
        question.setAnswerSourcePage(payload.answerSourcePage);
        examQuestionRepository.save(question);

        return examQuestionAssembler.toQuestionVOs(Collections.singletonList(question)).get(0);
    }

    public Map<String, Object> generateBatchAnswers(Long courseId, Long userId) {
        courseService.getOwnedCourse(courseId, userId);
        List<ExamQuestion> unanswered = examQuestionRepository.findByCourseIdAndAnswerTextIsNullOrderByIdAsc(courseId);
        if (unanswered.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("total", 0);
            empty.put("success", 0);
            empty.put("failed", 0);
            return empty;
        }
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ExamQuestion question : unanswered) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    generateAnswer(question.getId(), userId);
                    success.incrementAndGet();
                } catch (Exception exception) {
                    log.warn("Failed to generate answer for questionId={}, error={}",
                            question.getId(), exception.getMessage());
                    failed.incrementAndGet();
                }
            }, BATCH_ANSWER_EXECUTOR));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("total", unanswered.size());
        resultMap.put("success", success.get());
        resultMap.put("failed", failed.get());
        return resultMap;
    }

    private int materialTypePriority(String materialType) {
        if ("SLIDE".equalsIgnoreCase(materialType)) {
            return 0;
        }
        if ("NOTE".equalsIgnoreCase(materialType)) {
            return 1;
        }
        return 2;
    }

    private List<TextChunk> selectRelevantChunks(
            String questionText,
            List<TextChunk> allChunks,
            Map<Long, Material> materialById) {
        final int maxTotalChars = 2000;
        String lowerQuestion = questionText == null ? "" : questionText.toLowerCase(Locale.ROOT);
        List<ChunkCandidate> candidates = new ArrayList<>();
        for (TextChunk chunk : allChunks) {
            String content = chunk.getContent();
            if (content == null || content.trim().isEmpty()) {
                continue;
            }
            String lowerContent = content.toLowerCase(Locale.ROOT);
            int score = relevanceScore(lowerQuestion, lowerContent);
            if (score > 0) {
                Material material = materialById.get(chunk.getMaterialId());
                candidates.add(new ChunkCandidate(chunk, score, material != null ? material.getMaterialType() : null));
            }
        }
        candidates.sort((a, b) -> {
            int typeCompare = Integer.compare(materialTypePriority(a.materialType), materialTypePriority(b.materialType));
            if (typeCompare != 0) {
                return typeCompare;
            }
            int scoreCompare = Integer.compare(b.score, a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Integer.compare(a.chunk.getPageNo() != null ? a.chunk.getPageNo() : 0,
                    b.chunk.getPageNo() != null ? b.chunk.getPageNo() : 0);
        });
        List<TextChunk> selected = new ArrayList<>();
        int totalChars = 0;
        for (ChunkCandidate candidate : candidates) {
            int chunkLen = candidate.chunk.getContent().length();
            if (totalChars + chunkLen > maxTotalChars && !selected.isEmpty()) {
                continue;
            }
            selected.add(candidate.chunk);
            totalChars += chunkLen;
        }
        return selected;
    }

    private int relevanceScore(String lowerQuestion, String lowerContent) {
        int score = 0;
        String[] questionTokens = lowerQuestion.replaceAll("[\\p{Punct}\\p{Blank}，。、；：！？…—]", " ")
                .replaceAll("\\s+", " ").trim().split(" ");
        for (String token : questionTokens) {
            if (token.length() < 2) {
                continue;
            }
            if (lowerContent.contains(token)) {
                score += token.length();
            }
        }
        return score;
    }

    private String buildAnswerGenSystemPrompt() {
        return "You are AI4Note's exam answer generation assistant. "
                + "Generate a concise, accurate answer for the given exam question. "
                + "Return a strict JSON object with the shape {\"answerText\":\"...\",\"answerSource\":\"...\",\"answerSourcePage\":null}. "
                + "answerText is required. "
                + "For answerSource, follow these rules:\n"
                + "1. If the provided excerpts contain enough information to answer, "
                + "set answerSource to the exact \"Source:\" header line formatted as \"TYPE:TITLE\", "
                + "e.g. \"SLIDE:计算机网络课件\". Set answerSourcePage from the \"Page:\" header.\n"
                + "2. If the excerpts are not sufficient, you may use your own knowledge to answer, "
                + "but answerSource MUST clearly state: "
                + "\"上传资料中未找到相关内容，以下答案由 AI 综合知识生成\". "
                + "Set answerSourcePage to null in this case.\n"
                + "3. Never invent a fake reference — never cite a textbook, author, or paper "
                + "that does not appear in the excerpt headers. "
                + "Do not output markdown or explanations.";
    }

    private String buildAnswerGenUserPrompt(
            ExamQuestion question,
            List<TextChunk> chunks,
            Map<Long, Material> materialById) {
        StringBuilder builder = new StringBuilder();
        builder.append("Question type: ").append(
                question.getQuestionType() != null ? question.getQuestionType() : "UNKNOWN").append('\n');
        builder.append("Question text:\n").append(question.getQuestionText()).append("\n\n");
        builder.append("Reference material excerpts:\n");
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            Material material = materialById.get(chunk.getMaterialId());
            builder.append("---- Excerpt ").append(i + 1).append(" ----\n");
            if (material != null) {
                builder.append("Source: ").append(material.getTitle())
                        .append(" (").append(material.getMaterialType()).append(")\n");
            }
            if (chunk.getPageNo() != null) {
                builder.append("Page: ").append(chunk.getPageNo()).append('\n');
            }
            builder.append(chunk.getContent()).append('\n');
        }
        return builder.toString();
    }

    private AnswerGenPayload parseAnswerGenJson(String json) {
        try {
            JsonNode root = parseAiJson(json);
            AnswerGenPayload payload = new AnswerGenPayload();
            payload.answerText = examQuestionNormalizer.normalizeOptionalText(root.path("answerText").asText(null));
            payload.answerSource = examQuestionNormalizer.normalizeOptionalText(root.path("answerSource").asText(null));
            JsonNode pageNode = root.path("answerSourcePage");
            payload.answerSourcePage = pageNode.isNull() || pageNode.isMissingNode() ? null : pageNode.asInt();
            if (payload.answerText == null || payload.answerText.trim().isEmpty()) {
                throw new BusinessException("AI did not return a valid answer");
            }
            return payload;
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse answer-gen JSON, raw={}", abbreviateForLog(json), exception);
            throw new BusinessException("AI answer generation JSON could not be parsed");
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

    private static class ChunkCandidate {
        private final TextChunk chunk;
        private final int score;
        private final String materialType;

        private ChunkCandidate(TextChunk chunk, int score, String materialType) {
            this.chunk = chunk;
            this.score = score;
            this.materialType = materialType;
        }
    }

    private static class AnswerGenPayload {
        private String answerText;
        private String answerSource;
        private Integer answerSourcePage;
    }
}

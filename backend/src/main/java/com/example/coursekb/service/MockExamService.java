package com.example.coursekb.service;

import com.example.coursekb.dto.MockExamGenerateRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.CourseRelation;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.TeacherProfile;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.AiGenerationTaskRepository;
import com.example.coursekb.mapper.CourseRelationRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.TeacherProfileRepository;
import com.example.coursekb.vo.AiGenerationTaskVO;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.MockExamGenerateResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MockExamService {
    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> DIFFICULTIES = new HashSet<>(Arrays.asList(
            "EASY", "MEDIUM", "MEDIUM_HARD", "HARD"));
    private static final int MAX_KNOWLEDGE_ITEMS = 120;
    private static final int MAX_PREREQUISITE_KNOWLEDGE_ITEMS = 30;
    private static final int MAX_STATS = 40;
    private static final int MAX_KNOWLEDGE_CONTENT_LENGTH = 500;

    private final CourseService courseService;
    private final TeacherProfileRepository teacherProfileRepository;
    private final CourseRelationRepository courseRelationRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final ExamQuestionService examQuestionService;
    private final DeepSeekService deepSeekService;
    private final AiGenerationTaskService aiGenerationTaskService;
    private final AiGenerationTaskRepository aiGenerationTaskRepository;
    private final MaterialService materialService;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public MockExamService(
            CourseService courseService,
            TeacherProfileRepository teacherProfileRepository,
            CourseRelationRepository courseRelationRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            ExamQuestionService examQuestionService,
            DeepSeekService deepSeekService,
            AiGenerationTaskService aiGenerationTaskService,
            AiGenerationTaskRepository aiGenerationTaskRepository,
            MaterialService materialService,
            ObjectMapper objectMapper,
            @Value("${ai4note.storage-root}") String storageRoot) {
        this.courseService = courseService;
        this.teacherProfileRepository = teacherProfileRepository;
        this.courseRelationRepository = courseRelationRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.examQuestionService = examQuestionService;
        this.deepSeekService = deepSeekService;
        this.aiGenerationTaskService = aiGenerationTaskService;
        this.aiGenerationTaskRepository = aiGenerationTaskRepository;
        this.materialService = materialService;
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public MockExamGenerateResultVO generate(Long courseId, MockExamGenerateRequest request) {
        Course course = courseService.getOwnedCourse(courseId, request.getUserId());
        TeacherProfile profile = getOwnedProfile(request.getTeacherProfileId(), request.getUserId(), courseId);
        int questionCount = normalizeQuestionCount(request.getQuestionCount());
        String difficulty = normalizeDifficulty(request.getDifficultyLevel());

        List<KnowledgeItem> knowledgeItems =
                knowledgeItemRepository.findByCourseIdOrderByImportanceLevelDescIdDesc(courseId);
        if (knowledgeItems.isEmpty()) {
            throw new BusinessException("请先生成课程知识点，再生成模拟题");
        }
        List<ExamKnowledgeStatVO> stats = examQuestionService.listKnowledgeStats(courseId, request.getUserId(), null, null, null);
        List<KnowledgeItem> promptItems = selectPromptKnowledgeItems(knowledgeItems, stats);
        List<PrerequisiteKnowledgeGroup> prerequisiteKnowledgeGroups = loadPrerequisiteKnowledge(
                course,
                request.getUserId(),
                shouldIncludePrerequisites(request));

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(
                course,
                profile,
                promptItems,
                prerequisiteKnowledgeGroups,
                stats,
                questionCount,
                difficulty,
                request.getCustomRequirement());
        String prompt = systemPrompt + "\n\n" + userPrompt;
        AiGenerationTask task = aiGenerationTaskService.createTask(
                request.getUserId(),
                courseId,
                "MOCK_EXAM",
                prompt,
                null,
                profile.getId(),
                null);
        aiGenerationTaskService.markRunning(task.getId());

        try {
            String json = deepSeekService.generateJson(
                    request.getUserId(),
                    courseId,
                    systemPrompt,
                    userPrompt,
                    request.getModel(),
                    8192);
            JsonNode root = parseJson(json);
            JsonNode questions = questionsNode(root);
            if (!questions.isArray() || questions.isEmpty()) {
                throw new BusinessException("AI 未生成有效模拟题");
            }
            String markdown = renderMarkdown(course, profile, root, questions, stats, difficulty);
            Path outputPath = buildOutputPath(request.getUserId(), courseId, task.getId());
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, markdown.getBytes(StandardCharsets.UTF_8));
            String resultPath = storageRoot.relativize(outputPath).toString().replace('\\', '/');
            aiGenerationTaskService.markSuccess(task.getId(), resultPath);
            AiGenerationTask savedTask = aiGenerationTaskRepository.findById(task.getId()).orElse(task);
            String title = textOrDefault(root.path("title"), course.getCourseName() + " 模拟题");
            return new MockExamGenerateResultVO(
                    AiGenerationTaskVO.from(savedTask),
                    title,
                    resultPath,
                    markdown,
                    questions.size(),
                    materialService.registerGeneratedFile(
                            request.getUserId(),
                            courseId,
                            title,
                            "EXAM",
                            "AI generated mock exam Markdown",
                            outputPath));
        } catch (IOException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw new BusinessException("模拟题结果文件保存失败");
        } catch (RuntimeException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    public Path resolveDownloadPath(Long taskId, Long userId) {
        AiGenerationTask task = getOwnedMockExamTask(taskId, userId);
        if (!"SUCCESS".equals(task.getStatus()) || task.getResultPath() == null || task.getResultPath().trim().isEmpty()) {
            throw new BusinessException("模拟题结果尚未生成");
        }
        Path path = storageRoot.resolve(task.getResultPath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new BusinessException("模拟题结果文件不存在");
        }
        return path;
    }

    public String buildDownloadFilename(Long taskId, Long userId) {
        AiGenerationTask task = getOwnedMockExamTask(taskId, userId);
        return "mock-exam-" + task.getCourseId() + "-" + task.getId() + ".md";
    }

    private AiGenerationTask getOwnedMockExamTask(Long taskId, Long userId) {
        AiGenerationTask task = aiGenerationTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("模拟题任务不存在或无权访问"));
        courseService.getOwnedCourse(task.getCourseId(), userId);
        if (!"MOCK_EXAM".equals(task.getTaskType())) {
            throw new BusinessException("该任务不是模拟题生成任务");
        }
        return task;
    }

    private TeacherProfile getOwnedProfile(Long profileId, Long userId, Long courseId) {
        TeacherProfile profile = teacherProfileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new BusinessException("教师画像不存在或无权访问"));
        if (!courseId.equals(profile.getCourseId())) {
            throw new BusinessException("教师画像不属于当前课程");
        }
        return profile;
    }

    private int normalizeQuestionCount(Integer value) {
        int normalized = value == null ? 8 : value;
        if (normalized < 1 || normalized > 30) {
            throw new BusinessException("题目数量必须在 1 到 30 之间");
        }
        return normalized;
    }

    private String normalizeDifficulty(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "MEDIUM"
                : value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(normalized)) {
            throw new BusinessException("不支持的模拟题难度：" + normalized);
        }
        return normalized;
    }

    private List<KnowledgeItem> selectPromptKnowledgeItems(
            List<KnowledgeItem> knowledgeItems,
            List<ExamKnowledgeStatVO> stats) {
        Map<Long, KnowledgeItem> byId = knowledgeItems.stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, item -> item, (left, right) -> left));
        LinkedHashMap<Long, KnowledgeItem> selected = new LinkedHashMap<>();
        for (ExamKnowledgeStatVO stat : stats.stream().limit(MAX_STATS).collect(Collectors.toList())) {
            KnowledgeItem item = byId.get(stat.getKnowledgeItemId());
            if (item != null) {
                selected.put(item.getId(), item);
            }
        }
        for (KnowledgeItem item : knowledgeItems) {
            selected.putIfAbsent(item.getId(), item);
            if (selected.size() >= MAX_KNOWLEDGE_ITEMS) {
                break;
            }
        }
        return new ArrayList<>(selected.values());
    }

    private List<PrerequisiteKnowledgeGroup> loadPrerequisiteKnowledge(
            Course course,
            Long userId,
            boolean includePrerequisites) {
        if (!includePrerequisites) {
            return Collections.emptyList();
        }
        List<CourseRelation> relations = courseRelationRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId())
                .stream()
                .filter(relation -> "PREREQUISITE".equals(relation.getRelationType()))
                .collect(Collectors.toList());
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        int perCourseLimit = Math.max(5, MAX_PREREQUISITE_KNOWLEDGE_ITEMS / relations.size());
        int remaining = MAX_PREREQUISITE_KNOWLEDGE_ITEMS;
        List<PrerequisiteKnowledgeGroup> groups = new ArrayList<>();
        for (CourseRelation relation : relations) {
            if (remaining <= 0) {
                break;
            }
            Course prerequisite = courseService.getOwnedCourse(relation.getRelatedCourseId(), userId);
            List<KnowledgeItem> items = knowledgeItemRepository
                    .findByCourseIdOrderByImportanceLevelDescIdDesc(prerequisite.getId())
                    .stream()
                    .limit(Math.min(perCourseLimit, remaining))
                    .collect(Collectors.toList());
            if (!items.isEmpty()) {
                groups.add(new PrerequisiteKnowledgeGroup(relation, prerequisite, items));
                remaining -= items.size();
            }
        }
        return groups;
    }

    private String buildSystemPrompt() {
        return "You are AI4Note's mock exam generation assistant. "
                + "Generate a Chinese mock exam strictly from the provided teacher profile, knowledge items, and exam frequency stats. "
                + "Return only a valid JSON object with the shape "
                + "{\"title\":\"...\",\"instructions\":\"...\",\"questions\":["
                + "{\"questionNo\":\"1\",\"questionType\":\"...\",\"difficultyLevel\":\"...\",\"score\":10,"
                + "\"knowledgeTitle\":\"...\",\"questionText\":\"...\",\"answerOutline\":\"...\",\"gradingRubric\":\"...\"}]}. "
                + "Do not invent facts outside the supplied course context. "
                + "Use the teacher profile to imitate exam style, preferred question types, grading preference, focus topics, and avoid topics.";
    }

    private String buildUserPrompt(
            Course course,
            TeacherProfile profile,
            List<KnowledgeItem> knowledgeItems,
            List<PrerequisiteKnowledgeGroup> prerequisiteKnowledgeGroups,
            List<ExamKnowledgeStatVO> stats,
            int questionCount,
            String difficulty,
            String customRequirement) {
        StringBuilder builder = new StringBuilder();
        builder.append("Course:\n");
        builder.append("- name: ").append(course.getCourseName()).append('\n');
        if (course.getCourseCode() != null) {
            builder.append("- code: ").append(course.getCourseCode()).append('\n');
        }
        if (course.getSemester() != null) {
            builder.append("- semester: ").append(course.getSemester()).append('\n');
        }
        builder.append("\nTeacher profile:\n");
        builder.append("- teacherName: ").append(profile.getTeacherName()).append('\n');
        appendOptional(builder, "- analysisStatus: ", profile.getAnalysisStatus());
        appendOptional(builder, "- confidenceScore: ", profile.getConfidenceScore());
        appendOptional(builder, "- examStyle: ", profile.getExamStyle());
        appendOptional(builder, "- questionPreference: ", profile.getQuestionPreference());
        appendOptional(builder, "- gradingPreference: ", profile.getGradingPreference());
        appendOptional(builder, "- focusTopics: ", profile.getFocusTopics());
        appendOptional(builder, "- avoidTopics: ", profile.getAvoidTopics());
        appendOptional(builder, "- sourceSummary: ", profile.getSourceSummary());

        builder.append("\nExam frequency stats, sorted by frequency:\n");
        if (stats.isEmpty()) {
            builder.append("- No mapped historical exam stats. Use knowledge importance and teacher profile instead.\n");
        } else {
            for (ExamKnowledgeStatVO stat : stats.stream().limit(MAX_STATS).collect(Collectors.toList())) {
                builder.append("- knowledge: ").append(stat.getKnowledgeTitle())
                        .append("; itemType: ").append(nullToDash(stat.getKnowledgeItemType()))
                        .append("; chapter: ").append(nullToDash(stat.getChapterTitle()))
                        .append("; questionCount: ").append(stat.getQuestionCount())
                        .append("; totalScore: ").append(stat.getTotalScore() == null ? "-" : stat.getTotalScore())
                        .append("; latestYear: ").append(stat.getLatestExamYear() == null ? "-" : stat.getLatestExamYear())
                        .append('\n');
            }
        }

        builder.append("\nCandidate knowledge items:\n");
        for (KnowledgeItem item : knowledgeItems) {
            appendKnowledgeItem(builder, item);
        }

        builder.append("\nPrerequisite knowledge items:\n");
        if (prerequisiteKnowledgeGroups.isEmpty()) {
            builder.append("- No prerequisite knowledge items are supplied. Do not invent prerequisite content.\n");
        } else {
            for (PrerequisiteKnowledgeGroup group : prerequisiteKnowledgeGroups) {
                builder.append("- course: ").append(group.course.getCourseName());
                if (group.course.getCourseCode() != null && !group.course.getCourseCode().trim().isEmpty()) {
                    builder.append(" (").append(group.course.getCourseCode().trim()).append(")");
                }
                appendInlineOptional(builder, "; relationReason: ", group.relation.getReason());
                builder.append('\n');
                for (KnowledgeItem item : group.items) {
                    builder.append("  ");
                    appendKnowledgeItem(builder, item);
                }
            }
        }

        builder.append("\nGeneration requirements:\n");
        builder.append("- questionCount: ").append(questionCount).append('\n');
        builder.append("- difficultyLevel: ").append(difficulty).append('\n');
        if (prerequisiteKnowledgeGroups.isEmpty()) {
            builder.append("- Generate questions from current course knowledge only.\n");
        } else {
            builder.append("- Target question focus: current course 80%-90%, prerequisite foundations 10%-20%.\n");
            builder.append("- Prerequisite items should appear only as foundation checks or context needed to solve current-course questions.\n");
        }
        builder.append("- Include a balanced set of objective and subjective questions when the teacher profile supports it.\n");
        builder.append("- Prioritize high-frequency historical exam knowledge, then high-importance knowledge items.\n");
        builder.append("- For each question, provide answerOutline and gradingRubric.\n");
        builder.append("- Use Chinese for the exam content.\n");
        if (customRequirement != null && !customRequirement.trim().isEmpty()) {
            builder.append("- Custom requirement: ").append(customRequirement.trim()).append('\n');
        }
        return builder.toString();
    }

    private String renderMarkdown(
            Course course,
            TeacherProfile profile,
            JsonNode root,
            JsonNode questions,
            List<ExamKnowledgeStatVO> stats,
            String difficulty) {
        String title = textOrDefault(root.path("title"), course.getCourseName() + " 模拟题");
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(title).append("\n\n");
        builder.append("- 课程：").append(course.getCourseName()).append('\n');
        builder.append("- 教师画像：").append(profile.getTeacherName()).append('\n');
        builder.append("- 难度：").append(difficulty).append('\n');
        builder.append("- 生成时间：").append(LocalDateTime.now()).append("\n\n");
        String instructions = textOrDefault(root.path("instructions"), null);
        if (instructions != null) {
            builder.append("## 作答说明\n\n").append(instructions).append("\n\n");
        }
        builder.append("## 试题\n\n");
        int index = 1;
        for (JsonNode question : questions) {
            builder.append("### ").append(index).append(". ")
                    .append(textOrDefault(question.path("questionType"), "试题")).append('\n');
            appendMarkdownLine(builder, "知识点", textOrDefault(question.path("knowledgeTitle"), null));
            appendMarkdownLine(builder, "难度", textOrDefault(question.path("difficultyLevel"), difficulty));
            appendMarkdownLine(builder, "分值", decimalText(question.path("score")));
            builder.append('\n')
                    .append(textOrDefault(question.path("questionText"), ""))
                    .append("\n\n");
            appendMarkdownBlock(builder, "参考答案", textOrDefault(question.path("answerOutline"), null));
            appendMarkdownBlock(builder, "评分要点", textOrDefault(question.path("gradingRubric"), null));
            index++;
        }
        if (!stats.isEmpty()) {
            builder.append("## 高频依据\n\n");
            for (ExamKnowledgeStatVO stat : stats.stream()
                    .sorted(Comparator.comparingLong(ExamKnowledgeStatVO::getQuestionCount).reversed())
                    .limit(10)
                    .collect(Collectors.toList())) {
                builder.append("- ").append(stat.getKnowledgeTitle())
                        .append("：").append(stat.getQuestionCount()).append(" 次");
                if (stat.getTotalScore() != null) {
                    builder.append("，累计 ").append(stat.getTotalScore()).append(" 分");
                }
                if (stat.getLatestExamYear() != null) {
                    builder.append("，最近年份 ").append(stat.getLatestExamYear());
                }
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private Path buildOutputPath(Long userId, Long courseId, Long taskId) {
        String fileName = "mock-exam-" + FILE_TIME_FORMAT.format(LocalDateTime.now())
                + "-task-" + taskId + ".md";
        Path directory = storageRoot.resolve("mock-exams")
                .resolve("user-" + userId)
                .resolve("course-" + courseId);
        Path output = directory.resolve(fileName).normalize();
        if (!output.startsWith(directory.normalize()) || !output.startsWith(storageRoot)) {
            throw new BusinessException("模拟题文件路径不合法");
        }
        return output;
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(normalizeAiJson(value));
        } catch (JsonProcessingException exception) {
            String extracted = extractJsonBody(normalizeAiJson(value));
            if (extracted != null) {
                try {
                    return objectMapper.readTree(extracted);
                } catch (JsonProcessingException ignored) {
                    throw new BusinessException("AI 模拟题 JSON 解析失败");
                }
            }
            throw new BusinessException("AI 模拟题 JSON 解析失败");
        }
    }

    private JsonNode questionsNode(JsonNode root) {
        return root.isArray() ? root : root.path("questions");
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

    private void appendOptional(StringBuilder builder, String label, Object value) {
        if (value != null && !value.toString().trim().isEmpty()) {
            builder.append(label).append(value.toString().trim()).append('\n');
        }
    }

    private void appendInlineOptional(StringBuilder builder, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append(label).append(value.trim());
        }
    }

    private void appendKnowledgeItem(StringBuilder builder, KnowledgeItem item) {
        builder.append("- id: ").append(item.getId())
                .append("; title: ").append(item.getTitle())
                .append("; type: ").append(item.getItemType())
                .append("; importance: ").append(item.getImportanceLevel())
                .append("; content: ").append(abbreviate(item.getContent(), MAX_KNOWLEDGE_CONTENT_LENGTH))
                .append('\n');
    }

    private boolean shouldIncludePrerequisites(MockExamGenerateRequest request) {
        return !Boolean.FALSE.equals(request.getIncludePrerequisites());
    }

    private void appendMarkdownLine(StringBuilder builder, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append("- ").append(label).append("：").append(value.trim()).append('\n');
        }
    }

    private void appendMarkdownBlock(StringBuilder builder, String title, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append("**").append(title).append("**\n\n")
                    .append(value.trim()).append("\n\n");
        }
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        String value = node.asText();
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private String decimalText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            BigDecimal decimal = node.decimalValue();
            return decimal.stripTrailingZeros().toPlainString();
        }
        String value = node.asText();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private static class PrerequisiteKnowledgeGroup {
        private final CourseRelation relation;
        private final Course course;
        private final List<KnowledgeItem> items;

        private PrerequisiteKnowledgeGroup(CourseRelation relation, Course course, List<KnowledgeItem> items) {
            this.relation = relation;
            this.course = course;
            this.items = items;
        }
    }
}

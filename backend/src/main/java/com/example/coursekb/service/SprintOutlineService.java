package com.example.coursekb.service;

import com.example.coursekb.dto.SprintOutlineGenerateRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.TeacherProfile;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.AiGenerationTaskRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.TeacherProfileRepository;
import com.example.coursekb.vo.AiGenerationTaskVO;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.SprintOutlineGenerateResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SprintOutlineService {
    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_KNOWLEDGE_ITEMS = 80;
    private static final int MAX_STATS = 40;
    private static final int MAX_KNOWLEDGE_CONTENT_LENGTH = 420;

    private final CourseService courseService;
    private final TeacherProfileRepository teacherProfileRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final ExamQuestionService examQuestionService;
    private final DeepSeekService deepSeekService;
    private final AiGenerationTaskService aiGenerationTaskService;
    private final AiGenerationTaskRepository aiGenerationTaskRepository;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public SprintOutlineService(
            CourseService courseService,
            TeacherProfileRepository teacherProfileRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            ExamQuestionService examQuestionService,
            DeepSeekService deepSeekService,
            AiGenerationTaskService aiGenerationTaskService,
            AiGenerationTaskRepository aiGenerationTaskRepository,
            ObjectMapper objectMapper,
            @Value("${ai4note.storage-root}") String storageRoot) {
        this.courseService = courseService;
        this.teacherProfileRepository = teacherProfileRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.examQuestionService = examQuestionService;
        this.deepSeekService = deepSeekService;
        this.aiGenerationTaskService = aiGenerationTaskService;
        this.aiGenerationTaskRepository = aiGenerationTaskRepository;
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public SprintOutlineGenerateResultVO generate(Long courseId, SprintOutlineGenerateRequest request) {
        Course course = courseService.getOwnedCourse(courseId, request.getUserId());
        TeacherProfile profile = getOwnedProfile(request.getTeacherProfileId(), request.getUserId(), courseId);
        int days = normalizeDays(request.getDays());
        List<KnowledgeItem> knowledgeItems = knowledgeItemRepository
                .findByCourseIdOrderByImportanceLevelDescIdDesc(courseId)
                .stream()
                .limit(MAX_KNOWLEDGE_ITEMS)
                .collect(Collectors.toList());
        if (knowledgeItems.isEmpty()) {
            throw new BusinessException("请先生成课程知识点，再生成冲刺复习提纲");
        }
        List<ExamKnowledgeStatVO> stats = examQuestionService.listKnowledgeStats(
                courseId, request.getUserId(), null, null, null, null);

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(course, profile, knowledgeItems, stats, days, request.getCustomRequirement());
        String prompt = systemPrompt + "\n\n" + userPrompt;
        AiGenerationTask task = aiGenerationTaskService.createTask(
                request.getUserId(),
                courseId,
                "REVIEW_GENERATION",
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
            JsonNode daysNode = root.path("dailyPlan");
            if (!daysNode.isArray() || daysNode.isEmpty()) {
                throw new BusinessException("AI 未生成有效冲刺复习提纲");
            }
            String markdown = renderMarkdown(course, profile, root, daysNode, stats);
            Path outputPath = buildOutputPath(request.getUserId(), courseId, task.getId());
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, markdown.getBytes(StandardCharsets.UTF_8));
            String resultPath = storageRoot.relativize(outputPath).toString().replace('\\', '/');
            aiGenerationTaskService.markSuccess(task.getId(), resultPath);
            AiGenerationTask savedTask = aiGenerationTaskRepository.findById(task.getId()).orElse(task);
            return new SprintOutlineGenerateResultVO(
                    AiGenerationTaskVO.from(savedTask),
                    textOrDefault(root.path("title"), course.getCourseName() + " 冲刺复习提纲"),
                    resultPath,
                    markdown,
                    daysNode.size());
        } catch (IOException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw new BusinessException("冲刺复习提纲结果文件保存失败");
        } catch (RuntimeException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    public Path resolveDownloadPath(Long taskId, Long userId) {
        AiGenerationTask task = getOwnedSprintOutlineTask(taskId, userId);
        if (!"SUCCESS".equals(task.getStatus()) || task.getResultPath() == null || task.getResultPath().trim().isEmpty()) {
            throw new BusinessException("冲刺复习提纲尚未生成");
        }
        Path path = storageRoot.resolve(task.getResultPath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new BusinessException("冲刺复习提纲结果文件不存在");
        }
        return path;
    }

    public String buildDownloadFilename(Long taskId, Long userId) {
        AiGenerationTask task = getOwnedSprintOutlineTask(taskId, userId);
        return "sprint-outline-" + task.getCourseId() + "-" + task.getId() + ".md";
    }

    private AiGenerationTask getOwnedSprintOutlineTask(Long taskId, Long userId) {
        AiGenerationTask task = aiGenerationTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("冲刺复习提纲任务不存在或无权访问"));
        courseService.getOwnedCourse(task.getCourseId(), userId);
        if (!"REVIEW_GENERATION".equals(task.getTaskType())
                || task.getResultPath() == null
                || !task.getResultPath().startsWith("sprint-outlines/")) {
            throw new BusinessException("该任务不是冲刺复习提纲生成任务");
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

    private int normalizeDays(Integer value) {
        int normalized = value == null ? 7 : value;
        if (normalized < 1 || normalized > 30) {
            throw new BusinessException("冲刺天数必须在 1 到 30 之间");
        }
        return normalized;
    }

    private String buildSystemPrompt() {
        return "You are AI4Note's sprint review planning assistant. "
                + "Generate a Chinese sprint review outline based on the teacher profile, course knowledge, and historical exam frequency. "
                + "Return only a valid JSON object with the shape "
                + "{\"title\":\"...\",\"overview\":\"...\",\"styleStrategy\":\"...\","
                + "\"highPriorityTopics\":[\"...\"],\"dailyPlan\":["
                + "{\"day\":1,\"title\":\"...\",\"focus\":\"...\",\"activities\":[\"...\"],\"expectedOutput\":\"...\"}]}. "
                + "The outline must reflect the teacher's exam style, question preference, grading preference, focus topics, and avoid topics.";
    }

    private String buildUserPrompt(
            Course course,
            TeacherProfile profile,
            List<KnowledgeItem> knowledgeItems,
            List<ExamKnowledgeStatVO> stats,
            int days,
            String customRequirement) {
        StringBuilder builder = new StringBuilder();
        builder.append("Course: ").append(course.getCourseName()).append('\n');
        appendOptional(builder, "Course code: ", course.getCourseCode());
        appendOptional(builder, "Semester: ", course.getSemester());
        builder.append("\nTeacher profile:\n");
        builder.append("- teacherName: ").append(profile.getTeacherName()).append('\n');
        appendOptional(builder, "- examStyle: ", profile.getExamStyle());
        appendOptional(builder, "- questionPreference: ", profile.getQuestionPreference());
        appendOptional(builder, "- gradingPreference: ", profile.getGradingPreference());
        appendOptional(builder, "- focusTopics: ", profile.getFocusTopics());
        appendOptional(builder, "- avoidTopics: ", profile.getAvoidTopics());
        appendOptional(builder, "- sourceSummary: ", profile.getSourceSummary());

        builder.append("\nHigh-frequency exam stats:\n");
        if (stats.isEmpty()) {
            builder.append("- No mapped historical exam stats. Use knowledge importance and teacher profile.\n");
        } else {
            for (ExamKnowledgeStatVO stat : stats.stream().limit(MAX_STATS).collect(Collectors.toList())) {
                builder.append("- ").append(stat.getKnowledgeTitle())
                        .append("; type: ").append(nullToDash(stat.getKnowledgeItemType()))
                        .append("; questionCount: ").append(stat.getQuestionCount())
                        .append("; totalScore: ").append(stat.getTotalScore() == null ? "-" : stat.getTotalScore())
                        .append("; latestYear: ").append(stat.getLatestExamYear() == null ? "-" : stat.getLatestExamYear())
                        .append('\n');
            }
        }

        builder.append("\nKnowledge items:\n");
        for (KnowledgeItem item : knowledgeItems) {
            builder.append("- ").append(item.getTitle())
                    .append("; type: ").append(item.getItemType())
                    .append("; importance: ").append(item.getImportanceLevel())
                    .append("; content: ").append(abbreviate(item.getContent(), MAX_KNOWLEDGE_CONTENT_LENGTH))
                    .append('\n');
        }

        builder.append("\nPlan requirements:\n");
        builder.append("- days: ").append(days).append('\n');
        builder.append("- Every day must include focus, activities, and expectedOutput.\n");
        builder.append("- Prioritize teacher focus topics and historical high-frequency topics.\n");
        builder.append("- Include practice tactics that match the teacher's preferred question style.\n");
        builder.append("- Use Chinese.\n");
        if (customRequirement != null && !customRequirement.trim().isEmpty()) {
            builder.append("- Custom requirement: ").append(customRequirement.trim()).append('\n');
        }
        return builder.toString();
    }

    private String renderMarkdown(
            Course course,
            TeacherProfile profile,
            JsonNode root,
            JsonNode daysNode,
            List<ExamKnowledgeStatVO> stats) {
        String title = textOrDefault(root.path("title"), course.getCourseName() + " 冲刺复习提纲");
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(title).append("\n\n");
        builder.append("- 课程：").append(course.getCourseName()).append('\n');
        builder.append("- 教师画像：").append(profile.getTeacherName()).append('\n');
        builder.append("- 生成时间：").append(LocalDateTime.now()).append("\n\n");
        appendMarkdownBlock(builder, "总览", textOrDefault(root.path("overview"), null));
        appendMarkdownBlock(builder, "出题风格策略", textOrDefault(root.path("styleStrategy"), null));
        JsonNode topics = root.path("highPriorityTopics");
        if (topics.isArray() && !topics.isEmpty()) {
            builder.append("## 高优先级主题\n\n");
            for (JsonNode topic : topics) {
                builder.append("- ").append(topic.asText()).append('\n');
            }
            builder.append('\n');
        }
        builder.append("## 每日安排\n\n");
        for (JsonNode day : daysNode) {
            builder.append("### Day ").append(textOrDefault(day.path("day"), "?"))
                    .append("：").append(textOrDefault(day.path("title"), "冲刺复习")).append("\n\n");
            appendMarkdownLine(builder, "重点", textOrDefault(day.path("focus"), null));
            JsonNode activities = day.path("activities");
            if (activities.isArray() && !activities.isEmpty()) {
                builder.append("**任务**\n\n");
                for (JsonNode activity : activities) {
                    builder.append("- ").append(activity.asText()).append('\n');
                }
                builder.append('\n');
            }
            appendMarkdownBlock(builder, "产出", textOrDefault(day.path("expectedOutput"), null));
        }
        if (!stats.isEmpty()) {
            builder.append("## 高频考点参考\n\n");
            for (ExamKnowledgeStatVO stat : stats.stream()
                    .sorted(Comparator.comparingLong(ExamKnowledgeStatVO::getQuestionCount).reversed())
                    .limit(10)
                    .collect(Collectors.toList())) {
                builder.append("- ").append(stat.getKnowledgeTitle())
                        .append("：").append(stat.getQuestionCount()).append(" 次");
                if (stat.getLatestExamYear() != null) {
                    builder.append("，最近年份 ").append(stat.getLatestExamYear());
                }
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    private Path buildOutputPath(Long userId, Long courseId, Long taskId) {
        String fileName = "sprint-outline-" + FILE_TIME_FORMAT.format(LocalDateTime.now())
                + "-task-" + taskId + ".md";
        Path directory = storageRoot.resolve("sprint-outlines")
                .resolve("user-" + userId)
                .resolve("course-" + courseId);
        Path output = directory.resolve(fileName).normalize();
        if (!output.startsWith(directory.normalize()) || !output.startsWith(storageRoot)) {
            throw new BusinessException("冲刺复习提纲文件路径不合法");
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
                    throw new BusinessException("AI 冲刺复习提纲 JSON 解析失败");
                }
            }
            throw new BusinessException("AI 冲刺复习提纲 JSON 解析失败");
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

    private void appendOptional(StringBuilder builder, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append(label).append(value.trim()).append('\n');
        }
    }

    private void appendMarkdownLine(StringBuilder builder, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append("- ").append(label).append("：").append(value.trim()).append("\n\n");
        }
    }

    private void appendMarkdownBlock(StringBuilder builder, String title, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append("## ").append(title).append("\n\n")
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
}

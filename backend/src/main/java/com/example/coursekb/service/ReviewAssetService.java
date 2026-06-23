package com.example.coursekb.service;

import com.example.coursekb.dto.ReviewAssetGenerateRequest;
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
import com.example.coursekb.vo.ReviewAssetGenerateResultVO;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReviewAssetService {
    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> OUTPUT_TYPES = new HashSet<>(Arrays.asList(
            "REVIEW_NOTE", "OUTLINE", "FLASHCARDS", "CHECKLIST"));
    private static final int MAX_CURRENT_KNOWLEDGE_ITEMS = 150;
    private static final int MAX_PREREQUISITE_KNOWLEDGE_ITEMS = 50;
    private static final int MAX_STATS = 40;
    private static final int MAX_KNOWLEDGE_CONTENT_LENGTH = 520;

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

    public ReviewAssetService(
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

    public ReviewAssetGenerateResultVO generate(Long courseId, ReviewAssetGenerateRequest request) {
        Course course = courseService.getOwnedCourse(courseId, request.getUserId());
        String outputType = normalizeOutputType(request.getOutputType());
        TeacherProfile profile = getOwnedProfile(request.getTeacherProfileId(), request.getUserId(), courseId);
        List<KnowledgeItem> knowledgeItems = knowledgeItemRepository
                .findByCourseIdOrderByImportanceLevelDescIdDesc(courseId)
                .stream()
                .limit(MAX_CURRENT_KNOWLEDGE_ITEMS)
                .collect(Collectors.toList());
        if (knowledgeItems.isEmpty()) {
            throw new BusinessException("请先生成课程知识点，再生成复习资料");
        }
        List<PrerequisiteKnowledgeGroup> prerequisiteKnowledgeGroups = loadPrerequisiteKnowledge(
                course,
                request.getUserId(),
                shouldIncludePrerequisites(request));
        List<ExamKnowledgeStatVO> stats = examQuestionService.listKnowledgeStats(
                courseId, request.getUserId(), null, null, null);

        String systemPrompt = buildSystemPrompt(outputType);
        String userPrompt = buildUserPrompt(course, profile, knowledgeItems, prerequisiteKnowledgeGroups, stats, request);
        String prompt = systemPrompt + "\n\n" + userPrompt;
        AiGenerationTask task = aiGenerationTaskService.createTask(
                request.getUserId(),
                courseId,
                "REVIEW_GENERATION",
                prompt,
                null,
                profile == null ? null : profile.getId(),
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
            String title = textOrDefault(root.path("title"), course.getCourseName() + " " + outputTypeLabel(outputType));
            String markdown = textOrDefault(root.path("markdown"), null);
            if (markdown == null || markdown.trim().isEmpty()) {
                throw new BusinessException("AI 未生成有效 Markdown 内容");
            }
            Path outputPath = buildOutputPath(request.getUserId(), courseId, outputType, task.getId());
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, markdown.getBytes(StandardCharsets.UTF_8));
            String resultPath = storageRoot.relativize(outputPath).toString().replace('\\', '/');
            aiGenerationTaskService.markSuccess(task.getId(), resultPath);
            AiGenerationTask savedTask = aiGenerationTaskRepository.findById(task.getId()).orElse(task);
            String materialTitle = title.endsWith(outputTypeLabel(outputType))
                    ? title
                    : title + " - " + outputTypeLabel(outputType);
            return new ReviewAssetGenerateResultVO(
                    AiGenerationTaskVO.from(savedTask),
                    title,
                    outputType,
                    resultPath,
                    markdown,
                    materialService.registerGeneratedFile(
                            request.getUserId(),
                            courseId,
                            materialTitle,
                            "NOTE",
                            outputTypeLabel(outputType) + " / AI generated Markdown",
                            outputPath));
        } catch (IOException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw new BusinessException(outputTypeLabel(outputType) + "结果文件保存失败");
        } catch (RuntimeException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    public Path resolveDownloadPath(Long taskId, Long userId) {
        AiGenerationTask task = getOwnedReviewAssetTask(taskId, userId);
        if (!"SUCCESS".equals(task.getStatus()) || task.getResultPath() == null || task.getResultPath().trim().isEmpty()) {
            throw new BusinessException("复习资料尚未生成");
        }
        Path path = storageRoot.resolve(task.getResultPath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new BusinessException("复习资料结果文件不存在");
        }
        return path;
    }

    public String buildDownloadFilename(Long taskId, Long userId) {
        AiGenerationTask task = getOwnedReviewAssetTask(taskId, userId);
        return "review-asset-" + task.getCourseId() + "-" + task.getId() + ".md";
    }

    private AiGenerationTask getOwnedReviewAssetTask(Long taskId, Long userId) {
        AiGenerationTask task = aiGenerationTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new BusinessException("复习资料任务不存在或无权访问"));
        courseService.getOwnedCourse(task.getCourseId(), userId);
        if (!"REVIEW_GENERATION".equals(task.getTaskType())
                || task.getResultPath() == null
                || !task.getResultPath().startsWith("review-assets/")) {
            throw new BusinessException("该任务不是复习资料生成任务");
        }
        return task;
    }

    private TeacherProfile getOwnedProfile(Long profileId, Long userId, Long courseId) {
        if (profileId == null) {
            return null;
        }
        TeacherProfile profile = teacherProfileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new BusinessException("教师画像不存在或无权访问"));
        if (!courseId.equals(profile.getCourseId())) {
            throw new BusinessException("教师画像不属于当前课程");
        }
        return profile;
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
        int perCourseLimit = Math.max(8, MAX_PREREQUISITE_KNOWLEDGE_ITEMS / relations.size());
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

    private String buildSystemPrompt(String outputType) {
        return "You are AI4Note's Chinese review asset generation assistant. "
                + "Return only a valid JSON object with fields {\"title\":\"...\",\"markdown\":\"...\"}. "
                + "The markdown field must contain polished Markdown content for " + outputTypeLabel(outputType) + ". "
                + "Use headings, lists, tables where appropriate. Do not include code fences around the markdown. "
                + outputRules(outputType);
    }

    private String buildUserPrompt(
            Course course,
            TeacherProfile profile,
            List<KnowledgeItem> knowledgeItems,
            List<PrerequisiteKnowledgeGroup> prerequisiteKnowledgeGroups,
            List<ExamKnowledgeStatVO> stats,
            ReviewAssetGenerateRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("Course:\n");
        builder.append("- name: ").append(course.getCourseName()).append('\n');
        appendOptional(builder, "- code: ", course.getCourseCode());
        appendOptional(builder, "- semester: ", course.getSemester());
        appendOptional(builder, "- description: ", course.getDescription());

        builder.append("\nRequested output:\n");
        builder.append("- type: ").append(normalizeOutputType(request.getOutputType())).append('\n');
        builder.append("- typeLabel: ").append(outputTypeLabel(normalizeOutputType(request.getOutputType()))).append('\n');
        builder.append("- difficultyLevel: ").append(normalizeDifficulty(request.getDifficultyLevel())).append('\n');
        builder.append("- includePrerequisites: ").append(shouldIncludePrerequisites(request)).append('\n');
        builder.append("- currentCourseKnowledgeCount: ").append(knowledgeItems.size()).append('\n');
        builder.append("- prerequisiteKnowledgeCount: ").append(prerequisiteKnowledgeGroups.stream()
                .mapToInt(group -> group.items.size())
                .sum()).append('\n');

        builder.append("\nTeacher profile:\n");
        if (profile == null) {
            builder.append("- No teacher profile selected. Use course knowledge and exam stats.\n");
        } else {
            builder.append("- teacherName: ").append(profile.getTeacherName()).append('\n');
            appendOptional(builder, "- examStyle: ", profile.getExamStyle());
            appendOptional(builder, "- questionPreference: ", profile.getQuestionPreference());
            appendOptional(builder, "- gradingPreference: ", profile.getGradingPreference());
            appendOptional(builder, "- focusTopics: ", profile.getFocusTopics());
            appendOptional(builder, "- avoidTopics: ", profile.getAvoidTopics());
            appendOptional(builder, "- sourceSummary: ", profile.getSourceSummary());
        }

        builder.append("\nHigh-frequency exam stats:\n");
        if (stats.isEmpty()) {
            builder.append("- No mapped historical exam stats. Use importance levels.\n");
        } else {
            for (ExamKnowledgeStatVO stat : stats.stream().limit(MAX_STATS).collect(Collectors.toList())) {
                builder.append("- ").append(stat.getKnowledgeTitle())
                        .append("; type: ").append(nullToDash(stat.getKnowledgeItemType()))
                        .append("; chapter: ").append(nullToDash(stat.getChapterTitle()))
                        .append("; questionCount: ").append(stat.getQuestionCount())
                        .append("; totalScore: ").append(stat.getTotalScore() == null ? "-" : stat.getTotalScore())
                        .append("; latestYear: ").append(stat.getLatestExamYear() == null ? "-" : stat.getLatestExamYear())
                        .append('\n');
            }
        }

        builder.append("\nKnowledge scope rule:\n");
        builder.append("- Current course knowledge is the main body of the review asset.\n");
        if (prerequisiteKnowledgeGroups.isEmpty()) {
            builder.append("- No prerequisite knowledge items are supplied. Do not invent prerequisite content.\n");
        } else {
            builder.append("- Target content balance: current course 75%-85%, prerequisite foundations 15%-25%.\n");
            builder.append("- Use prerequisite items only to explain foundations, repair weak links, or add prerequisite recap sections.\n");
            builder.append("- Mark prerequisite content clearly with labels such as 前置基础 or 补基础.\n");
        }

        builder.append("\nCurrent course knowledge items:\n");
        for (KnowledgeItem item : knowledgeItems) {
            appendKnowledgeItem(builder, item);
        }

        if (!prerequisiteKnowledgeGroups.isEmpty()) {
            builder.append("\nPrerequisite course knowledge items:\n");
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

        builder.append("\nWriting requirements:\n");
        builder.append("- Use Chinese.\n");
        builder.append("- Keep the content exam-oriented and directly usable by a student.\n");
        builder.append("- Prioritize high-frequency exam stats and high-importance knowledge items.\n");
        builder.append("- If teacher profile exists, adapt focus and practice advice to it.\n");
        if (request.getCustomRequirement() != null && !request.getCustomRequirement().trim().isEmpty()) {
            builder.append("- Custom requirement: ").append(request.getCustomRequirement().trim()).append('\n');
        }
        return builder.toString();
    }

    private String outputRules(String outputType) {
        if ("REVIEW_NOTE".equals(outputType)) {
            return "For review notes, include sections: overview, key concepts, common mistakes, exam focus, quick recap.";
        }
        if ("OUTLINE".equals(outputType)) {
            return "For review outline, include hierarchical headings, chapter/topic order, priorities, and time allocation advice.";
        }
        if ("FLASHCARDS".equals(outputType)) {
            return "For flashcards, produce a Markdown table with columns: front, back, tag, priority.";
        }
        if ("CHECKLIST".equals(outputType)) {
            return "For checklist, produce Markdown checkboxes grouped by topic and include acceptance criteria.";
        }
        return "";
    }

    private Path buildOutputPath(Long userId, Long courseId, String outputType, Long taskId) {
        String fileName = outputType.toLowerCase(Locale.ROOT).replace('_', '-')
                + "-" + FILE_TIME_FORMAT.format(LocalDateTime.now())
                + "-task-" + taskId + ".md";
        Path directory = storageRoot.resolve("review-assets")
                .resolve("user-" + userId)
                .resolve("course-" + courseId);
        Path output = directory.resolve(fileName).normalize();
        if (!output.startsWith(directory.normalize()) || !output.startsWith(storageRoot)) {
            throw new BusinessException("复习资料文件路径不合法");
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
                    throw new BusinessException("AI 复习资料 JSON 解析失败");
                }
            }
            throw new BusinessException("AI 复习资料 JSON 解析失败");
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
        int start = raw.indexOf('{');
        if (start < 0) {
            return null;
        }
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
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return raw.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String normalizeOutputType(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "REVIEW_NOTE"
                : value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        if (!OUTPUT_TYPES.contains(normalized)) {
            throw new BusinessException("不支持的复习资料类型：" + normalized);
        }
        return normalized;
    }

    private String normalizeDifficulty(String value) {
        return value == null || value.trim().isEmpty()
                ? "MEDIUM"
                : value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String outputTypeLabel(String outputType) {
        if ("REVIEW_NOTE".equals(outputType)) return "复习笔记";
        if ("OUTLINE".equals(outputType)) return "复习提纲";
        if ("FLASHCARDS".equals(outputType)) return "记忆卡片";
        if ("CHECKLIST".equals(outputType)) return "检查清单";
        return "复习资料";
    }

    private void appendOptional(StringBuilder builder, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append(label).append(value.trim()).append('\n');
        }
    }

    private void appendInlineOptional(StringBuilder builder, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append(label).append(value.trim());
        }
    }

    private void appendKnowledgeItem(StringBuilder builder, KnowledgeItem item) {
        builder.append("- ").append(item.getTitle())
                .append("; type: ").append(item.getItemType())
                .append("; importance: ").append(item.getImportanceLevel())
                .append("; content: ").append(abbreviate(item.getContent(), MAX_KNOWLEDGE_CONTENT_LENGTH))
                .append('\n');
    }

    private boolean shouldIncludePrerequisites(ReviewAssetGenerateRequest request) {
        return !Boolean.FALSE.equals(request.getIncludePrerequisites());
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

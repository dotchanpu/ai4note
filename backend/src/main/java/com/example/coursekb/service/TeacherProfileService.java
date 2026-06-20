package com.example.coursekb.service;

import com.example.coursekb.dto.TeacherProfileAnalyzeRequest;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TeacherProfile;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TeacherProfileRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.TeacherProfileVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TeacherProfileService {
    private static final int MAX_SOURCE_LENGTH = 52000;

    private final CourseService courseService;
    private final DeepSeekService deepSeekService;
    private final MaterialRepository materialRepository;
    private final TextChunkRepository textChunkRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final ObjectMapper objectMapper;

    public TeacherProfileService(
            CourseService courseService,
            DeepSeekService deepSeekService,
            MaterialRepository materialRepository,
            TextChunkRepository textChunkRepository,
            ExamQuestionRepository examQuestionRepository,
            TeacherProfileRepository teacherProfileRepository,
            ObjectMapper objectMapper) {
        this.courseService = courseService;
        this.deepSeekService = deepSeekService;
        this.materialRepository = materialRepository;
        this.textChunkRepository = textChunkRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.objectMapper = objectMapper;
    }

    public List<TeacherProfileVO> list(Long courseId, Long userId) {
        courseService.getOwnedCourse(courseId, userId);
        return teacherProfileRepository.findByUserIdAndCourseIdOrderByUpdateTimeDescIdDesc(userId, courseId)
                .stream()
                .map(TeacherProfileVO::from)
                .collect(Collectors.toList());
    }

    public TeacherProfileVO analyze(Long courseId, TeacherProfileAnalyzeRequest request) {
        Course course = courseService.getOwnedCourse(courseId, request.getUserId());
        List<Material> materials = resolveMaterials(courseId, request.getMaterialIds());
        String source = buildSource(course, materials);
        if (source.trim().isEmpty()) {
            throw new BusinessException("请先解析课程资料或抽取真题后再分析教师画像");
        }

        TeacherProfile profile = new TeacherProfile();
        profile.setUserId(request.getUserId());
        profile.setCourseId(courseId);
        profile.setTeacherName(request.getTeacherName().trim());
        profile.setGeneratedByAi(true);
        profile.setAnalysisStatus("RUNNING");
        profile = teacherProfileRepository.save(profile);

        try {
            String json = deepSeekService.generateJson(
                    request.getUserId(),
                    courseId,
                    buildSystemPrompt(),
                    source,
                    request.getModel(),
                    8192);
            applyAnalysis(profile, parseJson(json));
            profile.setAnalysisStatus("SUCCESS");
            profile.setLastAnalyzedTime(LocalDateTime.now());
            return TeacherProfileVO.from(teacherProfileRepository.save(profile));
        } catch (RuntimeException exception) {
            profile.setAnalysisStatus("FAILED");
            profile.setSourceSummary("分析失败：" + exception.getMessage());
            profile.setLastAnalyzedTime(LocalDateTime.now());
            teacherProfileRepository.save(profile);
            throw exception;
        }
    }

    private List<Material> resolveMaterials(Long courseId, List<Long> materialIds) {
        List<Material> materials = materialRepository.findByCourseIdOrderByUploadTimeDesc(courseId);
        if (materialIds == null || materialIds.isEmpty()) {
            return materials;
        }
        Set<Long> requestedIds = new HashSet<>(materialIds);
        List<Material> selected = materials.stream()
                .filter(material -> requestedIds.contains(material.getId()))
                .collect(Collectors.toList());
        if (selected.size() != requestedIds.size()) {
            throw new BusinessException("分析资料不属于当前课程");
        }
        return selected;
    }

    private String buildSource(Course course, List<Material> materials) {
        StringBuilder source = new StringBuilder();
        appendLimited(source, "课程：" + course.getCourseName() + "\n");
        appendLimited(source, "课程说明：" + valueOrDefault(course.getDescription(), "无") + "\n\n");
        appendLimited(source, "## 课程资料\n\n");
        for (Material material : materials) {
            appendLimited(source, "### " + material.getTitle() + "\n");
            appendLimited(source, "- 类型：" + material.getMaterialType() + "\n");
            appendLimited(source, "- 年份：" + valueOrDefault(material.getYear(), "未知") + "\n");
            appendLimited(source, "- 摘要：" + valueOrDefault(material.getSummary(), "无") + "\n");
            for (TextChunk chunk : textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(material.getId())) {
                appendLimited(source, "[第" + valueOrDefault(chunk.getPageNo(), "?") + "页] "
                        + chunk.getContent() + "\n");
                if (source.length() >= MAX_SOURCE_LENGTH) {
                    return source.toString();
                }
            }
            appendLimited(source, "\n");
        }

        List<ExamQuestion> questions = new ArrayList<>(
                examQuestionRepository.findByCourseIdOrderByExamYearDescIdDesc(course.getId()));
        questions.sort(Comparator.comparing(ExamQuestion::getExamYear, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExamQuestion::getId, Comparator.reverseOrder()));
        appendLimited(source, "\n## 往年真题\n\n");
        for (ExamQuestion question : questions) {
            appendLimited(source, "- " + valueOrDefault(question.getExamYear(), "未知年份")
                    + " " + valueOrDefault(question.getQuestionType(), "题目")
                    + "，分值 " + valueOrDefault(question.getScore(), "未知")
                    + "：" + question.getQuestionText() + "\n");
            if (question.getAnswerText() != null && !question.getAnswerText().trim().isEmpty()) {
                appendLimited(source, "  参考答案：" + question.getAnswerText() + "\n");
            }
            if (source.length() >= MAX_SOURCE_LENGTH) {
                break;
            }
        }
        return source.toString();
    }

    private void appendLimited(StringBuilder builder, String value) {
        if (builder.length() >= MAX_SOURCE_LENGTH || value == null) {
            return;
        }
        int remaining = MAX_SOURCE_LENGTH - builder.length();
        builder.append(value, 0, Math.min(value.length(), remaining));
    }

    private String buildSystemPrompt() {
        return "你是课程考试分析师。请基于课程资料和往年真题归纳教师画像。"
                + "只输出合法 JSON 对象，不要输出 Markdown。格式必须为："
                + "{\"confidenceScore\":80,\"examStyle\":\"出题风格\","
                + "\"questionPreference\":\"题型偏好\",\"gradingPreference\":\"评分偏好\","
                + "\"focusTopics\":[\"重点章节或主题\"],\"avoidTopics\":[\"低优先内容\"],"
                + "\"sourceSummary\":\"分析依据摘要\"}。"
                + "要求：必须使用中文；confidenceScore 为 0-100；"
                + "只能依据给定资料和真题，不要虚构教师行为；"
                + "如果证据不足，需要降低置信度并在 sourceSummary 中说明。";
    }

    private void applyAnalysis(TeacherProfile profile, JsonNode root) {
        profile.setConfidenceScore(normalizeConfidence(root.path("confidenceScore")));
        profile.setExamStyle(text(root.path("examStyle")));
        profile.setQuestionPreference(text(root.path("questionPreference")));
        profile.setGradingPreference(text(root.path("gradingPreference")));
        profile.setFocusTopics(nodeToText(root.path("focusTopics")));
        profile.setAvoidTopics(nodeToText(root.path("avoidTopics")));
        profile.setSourceSummary(text(root.path("sourceSummary")));
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(stripCodeFence(value));
        } catch (Exception exception) {
            throw new BusinessException("AI 返回格式不正确，请重试");
        }
    }

    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private BigDecimal normalizeConfidence(JsonNode node) {
        BigDecimal value = node == null || !node.isNumber()
                ? new BigDecimal("50")
                : node.decimalValue();
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal max = new BigDecimal("100");
        return value.compareTo(max) > 0 ? max : value;
    }

    private String nodeToText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (!node.isArray()) {
            return text(node);
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = text(item);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return String.join("\n", values);
    }

    private String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText("").trim();
    }

    private String valueOrDefault(Object value, String fallback) {
        return value == null || value.toString().trim().isEmpty() ? fallback : value.toString();
    }
}

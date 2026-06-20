package com.example.coursekb.service;

import com.example.coursekb.dto.TeacherProfileAnalyzeRequest;
import com.example.coursekb.dto.TeacherProfileReanalyzeRequest;
import com.example.coursekb.dto.TeacherProfileUpdateRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TeacherProfile;
import com.example.coursekb.entity.TeacherProfileEvidence;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TeacherProfileEvidenceRepository;
import com.example.coursekb.mapper.TeacherProfileRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.TeacherProfileEvidenceVO;
import com.example.coursekb.vo.TeacherProfileVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TeacherProfileService {
    private static final int MAX_SOURCE_LENGTH = 52000;
    private static final Set<String> MANUAL_STATUSES = new HashSet<>(Arrays.asList(
            "SUCCESS", "FAILED", "MANUAL_REVIEWED"));

    private final CourseService courseService;
    private final DeepSeekService deepSeekService;
    private final AiGenerationTaskService aiGenerationTaskService;
    private final MaterialRepository materialRepository;
    private final TextChunkRepository textChunkRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final TeacherProfileEvidenceRepository teacherProfileEvidenceRepository;
    private final ObjectMapper objectMapper;

    public TeacherProfileService(
            CourseService courseService,
            DeepSeekService deepSeekService,
            AiGenerationTaskService aiGenerationTaskService,
            MaterialRepository materialRepository,
            TextChunkRepository textChunkRepository,
            ExamQuestionRepository examQuestionRepository,
            TeacherProfileRepository teacherProfileRepository,
            TeacherProfileEvidenceRepository teacherProfileEvidenceRepository,
            ObjectMapper objectMapper) {
        this.courseService = courseService;
        this.deepSeekService = deepSeekService;
        this.aiGenerationTaskService = aiGenerationTaskService;
        this.materialRepository = materialRepository;
        this.textChunkRepository = textChunkRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.teacherProfileEvidenceRepository = teacherProfileEvidenceRepository;
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
        String systemPrompt = buildSystemPrompt();
        String prompt = systemPrompt + "\n\n教师：" + request.getTeacherName().trim() + "\n\n" + source;
        profile.setUserId(request.getUserId());
        profile.setCourseId(courseId);
        profile.setTeacherName(request.getTeacherName().trim());
        profile.setGeneratedByAi(true);
        profile.setAnalysisStatus("RUNNING");
        profile = teacherProfileRepository.save(profile);
        AiGenerationTask task = aiGenerationTaskService.createTask(
                request.getUserId(),
                courseId,
                "TEACHER_PROFILE",
                prompt,
                null,
                profile.getId(),
                request.getProviderConfigId());
        aiGenerationTaskService.markRunning(task.getId());

        try {
            String json = deepSeekService.generateJson(
                    request.getUserId(),
                    courseId,
                    systemPrompt,
                    source,
                    request.getModel(),
                    8192);
            JsonNode root = parseJson(json);
            applyAnalysis(profile, root);
            profile.setAnalysisStatus("SUCCESS");
            profile.setLastAnalyzedTime(LocalDateTime.now());
            TeacherProfile saved = teacherProfileRepository.save(profile);
            saveEvidence(saved, root.path("evidence"), materials);
            aiGenerationTaskService.markSuccess(task.getId(), "teacher-profile:" + saved.getId());
            return TeacherProfileVO.from(saved);
        } catch (RuntimeException exception) {
            profile.setAnalysisStatus("FAILED");
            profile.setSourceSummary("分析失败：" + exception.getMessage());
            profile.setLastAnalyzedTime(LocalDateTime.now());
            teacherProfileRepository.save(profile);
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    public List<TeacherProfileEvidenceVO> listEvidence(Long profileId, Long userId) {
        TeacherProfile profile = getOwnedProfile(profileId, userId);
        courseService.getOwnedCourse(profile.getCourseId(), userId);
        List<TeacherProfileEvidence> evidenceList = teacherProfileEvidenceRepository
                .findByTeacherProfileIdOrderByConfidenceScoreDescIdAsc(profileId);
        if (evidenceList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Material> materialById = materialRepository.findAllById(
                        evidenceList.stream().map(TeacherProfileEvidence::getMaterialId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        return evidenceList.stream()
                .map(evidence -> {
                    Material material = materialById.get(evidence.getMaterialId());
                    return TeacherProfileEvidenceVO.from(
                            evidence,
                            material == null ? "未知资料" : material.getTitle(),
                            material == null ? null : material.getMaterialType());
                })
                .collect(Collectors.toList());
    }

    public TeacherProfileVO reanalyze(Long profileId, TeacherProfileReanalyzeRequest request) {
        TeacherProfile profile = getOwnedProfile(profileId, request.getUserId());
        Course course = courseService.getOwnedCourse(profile.getCourseId(), request.getUserId());
        List<Material> materials = resolveMaterials(course.getId(), request.getMaterialIds());
        String source = buildSource(course, materials);
        if (source.trim().isEmpty()) {
            throw new BusinessException("请先解析课程资料或抽取真题后再重新分析教师画像");
        }

        String systemPrompt = buildSystemPrompt();
        String prompt = systemPrompt + "\n\n教师：" + profile.getTeacherName() + "\n\n" + source;
        profile.setGeneratedByAi(true);
        profile.setAnalysisStatus("RUNNING");
        profile = teacherProfileRepository.save(profile);
        AiGenerationTask task = aiGenerationTaskService.createTask(
                request.getUserId(),
                course.getId(),
                "TEACHER_PROFILE",
                prompt,
                null,
                profile.getId(),
                request.getProviderConfigId());
        aiGenerationTaskService.markRunning(task.getId());

        try {
            String json = deepSeekService.generateJson(
                    request.getUserId(),
                    course.getId(),
                    systemPrompt,
                    source,
                    request.getModel(),
                    8192);
            JsonNode root = parseJson(json);
            applyAnalysis(profile, root);
            profile.setAnalysisStatus("SUCCESS");
            profile.setLastAnalyzedTime(LocalDateTime.now());
            TeacherProfile saved = teacherProfileRepository.save(profile);
            saveEvidence(saved, root.path("evidence"), materials);
            aiGenerationTaskService.markSuccess(task.getId(), "teacher-profile:" + saved.getId());
            return TeacherProfileVO.from(saved);
        } catch (RuntimeException exception) {
            profile.setAnalysisStatus("FAILED");
            profile.setSourceSummary("重新分析失败：" + exception.getMessage());
            profile.setLastAnalyzedTime(LocalDateTime.now());
            teacherProfileRepository.save(profile);
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    public TeacherProfileVO update(Long profileId, TeacherProfileUpdateRequest request) {
        TeacherProfile profile = getOwnedProfile(profileId, request.getUserId());
        courseService.getOwnedCourse(profile.getCourseId(), request.getUserId());
        if (request.getTeacherName() != null && !request.getTeacherName().trim().isEmpty()) {
            profile.setTeacherName(request.getTeacherName().trim());
        }
        profile.setConfidenceScore(normalizeConfidence(request.getConfidenceScore()));
        profile.setExamStyle(normalizeText(request.getExamStyle()));
        profile.setQuestionPreference(normalizeText(request.getQuestionPreference()));
        profile.setGradingPreference(normalizeText(request.getGradingPreference()));
        profile.setFocusTopics(normalizeText(request.getFocusTopics()));
        profile.setAvoidTopics(normalizeText(request.getAvoidTopics()));
        profile.setSourceSummary(normalizeText(request.getSourceSummary()));
        profile.setAnalysisStatus(normalizeManualStatus(request.getAnalysisStatus()));
        profile.setGeneratedByAi(false);
        return TeacherProfileVO.from(teacherProfileRepository.save(profile));
    }

    public TeacherProfileVO recalculateConfidence(Long profileId, Long userId) {
        TeacherProfile profile = getOwnedProfile(profileId, userId);
        courseService.getOwnedCourse(profile.getCourseId(), userId);
        List<TeacherProfileEvidence> evidenceList = teacherProfileEvidenceRepository
                .findByTeacherProfileIdOrderByConfidenceScoreDescIdAsc(profileId);
        profile.setConfidenceScore(calculateConfidence(profile, evidenceList));
        return TeacherProfileVO.from(teacherProfileRepository.save(profile));
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

    private TeacherProfile getOwnedProfile(Long profileId, Long userId) {
        return teacherProfileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new BusinessException("教师画像不存在或无权访问"));
    }

    private String buildSource(Course course, List<Material> materials) {
        StringBuilder source = new StringBuilder();
        appendLimited(source, "课程：" + course.getCourseName() + "\n");
        appendLimited(source, "课程说明：" + valueOrDefault(course.getDescription(), "无") + "\n\n");
        appendLimited(source, "## 课程资料\n\n");
        for (Material material : materials) {
            appendLimited(source, "### " + material.getTitle() + "\n");
            appendLimited(source, "- 资料ID：" + material.getId() + "\n");
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
                + "\"sourceSummary\":\"分析依据摘要\","
                + "\"evidence\":[{\"materialId\":1,\"evidenceType\":\"EXAM_STYLE|QUESTION_TYPE|GRADING|FOCUS_TOPIC|AVOID_TOPIC\","
                + "\"evidenceSummary\":\"证据摘要\",\"sourcePage\":1,\"confidenceScore\":80}]}。"
                + "要求：必须使用中文；confidenceScore 为 0-100；"
                + "evidence 最多 12 条，materialId 必须来自资料上下文中的资料ID；"
                + "只能依据给定资料和真题，不要虚构教师行为；"
                + "如果证据不足，需要降低置信度并在 sourceSummary 中说明。";
    }

    private void saveEvidence(TeacherProfile profile, JsonNode evidenceNode, List<Material> materials) {
        teacherProfileEvidenceRepository.deleteByTeacherProfileId(profile.getId());
        if (evidenceNode == null || !evidenceNode.isArray()) {
            return;
        }
        Set<Long> materialIds = materials.stream().map(Material::getId).collect(Collectors.toSet());
        List<TeacherProfileEvidence> evidenceList = new ArrayList<>();
        for (JsonNode node : evidenceNode) {
            Long materialId = node.path("materialId").canConvertToLong()
                    ? node.path("materialId").asLong()
                    : null;
            String summary = text(node.path("evidenceSummary"));
            if (materialId == null || !materialIds.contains(materialId) || summary.isEmpty()) {
                continue;
            }
            TeacherProfileEvidence evidence = new TeacherProfileEvidence();
            evidence.setTeacherProfileId(profile.getId());
            evidence.setMaterialId(materialId);
            evidence.setEvidenceType(normalizeEvidenceType(text(node.path("evidenceType"))));
            evidence.setEvidenceSummary(summary);
            evidence.setSourcePage(node.path("sourcePage").isInt() ? node.path("sourcePage").asInt() : null);
            evidence.setConfidenceScore(normalizeConfidence(node.path("confidenceScore")));
            evidenceList.add(evidence);
            if (evidenceList.size() >= 12) {
                break;
            }
        }
        teacherProfileEvidenceRepository.saveAll(evidenceList);
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
        return normalizeConfidence(value);
    }

    private BigDecimal normalizeConfidence(BigDecimal value) {
        if (value == null) {
            return new BigDecimal("50");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal max = new BigDecimal("100");
        return value.compareTo(max) > 0 ? max : value;
    }

    private BigDecimal calculateConfidence(
            TeacherProfile profile,
            List<TeacherProfileEvidence> evidenceList) {
        BigDecimal score = new BigDecimal("35");
        score = score.add(fieldScore(profile.getExamStyle(), 8));
        score = score.add(fieldScore(profile.getQuestionPreference(), 8));
        score = score.add(fieldScore(profile.getGradingPreference(), 8));
        score = score.add(fieldScore(profile.getFocusTopics(), 8));
        score = score.add(fieldScore(profile.getAvoidTopics(), 3));
        score = score.add(fieldScore(profile.getSourceSummary(), 5));

        int evidenceCount = evidenceList == null ? 0 : evidenceList.size();
        score = score.add(new BigDecimal(Math.min(24, evidenceCount * 4)));
        if (evidenceCount > 0) {
            BigDecimal averageEvidenceConfidence = evidenceList.stream()
                    .map(TeacherProfileEvidence::getConfidenceScore)
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(evidenceCount), 2, java.math.RoundingMode.HALF_UP);
            score = score.add(averageEvidenceConfidence.multiply(new BigDecimal("0.10")));
            long materialCount = evidenceList.stream()
                    .map(TeacherProfileEvidence::getMaterialId)
                    .distinct()
                    .count();
            score = score.add(new BigDecimal(Math.min(8, materialCount * 2)));
        }
        if ("MANUAL_REVIEWED".equals(profile.getAnalysisStatus())) {
            score = score.add(new BigDecimal("5"));
        }
        score = normalizeConfidence(score);
        if ("FAILED".equals(profile.getAnalysisStatus()) && score.compareTo(new BigDecimal("45")) > 0) {
            return new BigDecimal("45");
        }
        if ("RUNNING".equals(profile.getAnalysisStatus()) && score.compareTo(new BigDecimal("50")) > 0) {
            return new BigDecimal("50");
        }
        return score;
    }

    private BigDecimal fieldScore(String value, int maxScore) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        int length = value.trim().length();
        if (length >= 30) {
            return new BigDecimal(maxScore);
        }
        if (length >= 10) {
            return new BigDecimal(Math.max(1, maxScore - 2));
        }
        return new BigDecimal(Math.max(1, maxScore / 2));
    }

    private String normalizeManualStatus(String value) {
        String status = value == null || value.trim().isEmpty()
                ? "MANUAL_REVIEWED"
                : value.trim().toUpperCase();
        if (!MANUAL_STATUSES.contains(status)) {
            throw new BusinessException("不支持的教师画像状态：" + status);
        }
        return status;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEvidenceType(String value) {
        if ("QUESTION_TYPE".equals(value)
                || "GRADING".equals(value)
                || "FOCUS_TOPIC".equals(value)
                || "AVOID_TOPIC".equals(value)) {
            return value;
        }
        return "EXAM_STYLE";
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

package com.example.coursekb.service;

import com.example.coursekb.dto.AiGenerationTaskStatusRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.AiGenerationTaskRepository;
import com.example.coursekb.vo.AiGenerationTaskVO;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiGenerationTaskService {
    private static final Set<String> TASK_TYPES = new HashSet<>(Arrays.asList(
            "TEACHER_PROFILE", "EXAM_MAPPING", "KNOWLEDGE_GAP", "REVIEW_GENERATION",
            "MOCK_EXAM", "PACKAGE_SUMMARY", "KNOWLEDGE_EXTRACTION"));
    private static final Set<String> STATUSES = new HashSet<>(Arrays.asList(
            "PENDING", "RUNNING", "SUCCESS", "FAILED", "CANCELED"));

    private final CourseService courseService;
    private final AiGenerationTaskRepository aiGenerationTaskRepository;

    public AiGenerationTaskService(
            CourseService courseService,
            AiGenerationTaskRepository aiGenerationTaskRepository) {
        this.courseService = courseService;
        this.aiGenerationTaskRepository = aiGenerationTaskRepository;
    }

    public List<AiGenerationTaskVO> list(Long userId, Long courseId) {
        if (courseId != null) {
            courseService.getOwnedCourse(courseId, userId);
            return aiGenerationTaskRepository.findByUserIdAndCourseIdOrderByCreateTimeDescIdDesc(userId, courseId)
                    .stream()
                    .map(AiGenerationTaskVO::from)
                    .collect(Collectors.toList());
        }
        return aiGenerationTaskRepository.findByUserIdOrderByCreateTimeDescIdDesc(userId)
                .stream()
                .map(AiGenerationTaskVO::from)
                .collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiGenerationTask createTask(
            Long userId,
            Long courseId,
            String taskType,
            String prompt,
            Long reviewProfileId,
            Long teacherProfileId,
            Long providerConfigId) {
        courseService.getOwnedCourse(courseId, userId);
        AiGenerationTask task = new AiGenerationTask();
        task.setUserId(userId);
        task.setCourseId(courseId);
        task.setTaskType(normalizeTaskType(taskType));
        task.setPrompt(trimToLength(prompt, 60000));
        task.setReviewProfileId(reviewProfileId);
        task.setTeacherProfileId(teacherProfileId);
        task.setProviderConfigId(providerConfigId);
        task.setStatus("PENDING");
        return aiGenerationTaskRepository.save(task);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRunning(Long taskId) {
        updateStatusInternal(taskId, "RUNNING", null, null, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long taskId, String resultPath) {
        updateStatusInternal(taskId, "SUCCESS", resultPath, null, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long taskId, String errorMessage) {
        updateStatusInternal(taskId, "FAILED", null, errorMessage, true);
    }

    @Transactional
    public AiGenerationTaskVO updateStatus(Long taskId, AiGenerationTaskStatusRequest request) {
        AiGenerationTask task = aiGenerationTaskRepository.findByIdAndUserId(taskId, request.getUserId())
                .orElseThrow(() -> new BusinessException("AI 生成任务不存在或无权访问"));
        courseService.getOwnedCourse(task.getCourseId(), request.getUserId());
        applyStatus(task, normalizeStatus(request.getStatus()), request.getResultPath(), request.getErrorMessage());
        return AiGenerationTaskVO.from(aiGenerationTaskRepository.save(task));
    }

    private void updateStatusInternal(
            Long taskId, String status, String resultPath, String errorMessage, boolean finished) {
        if (taskId == null) {
            return;
        }
        AiGenerationTask task = aiGenerationTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        task.setStatus(status);
        task.setResultPath(trimToLength(resultPath, 512));
        task.setErrorMessage(trimToLength(errorMessage, 2000));
        if (finished) {
            task.setFinishTime(LocalDateTime.now());
        }
        aiGenerationTaskRepository.save(task);
    }

    private void applyStatus(AiGenerationTask task, String status, String resultPath, String errorMessage) {
        task.setStatus(status);
        task.setResultPath(trimToLength(resultPath, 512));
        task.setErrorMessage(trimToLength(errorMessage, 2000));
        if ("SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELED".equals(status)) {
            task.setFinishTime(LocalDateTime.now());
        }
    }

    private String normalizeTaskType(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "REVIEW_GENERATION"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!TASK_TYPES.contains(normalized)) {
            throw new BusinessException("不支持的 AI 任务类型：" + normalized);
        }
        return normalized;
    }

    private String normalizeStatus(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "PENDING"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new BusinessException("不支持的 AI 任务状态：" + normalized);
        }
        return normalized;
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}

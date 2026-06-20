package com.example.coursekb.vo;

import com.example.coursekb.entity.AiGenerationTask;
import java.time.LocalDateTime;

public class AiGenerationTaskVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private Long reviewProfileId;
    private Long teacherProfileId;
    private Long providerConfigId;
    private String taskType;
    private String prompt;
    private String status;
    private String resultPath;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;

    public static AiGenerationTaskVO from(AiGenerationTask task) {
        AiGenerationTaskVO result = new AiGenerationTaskVO();
        result.id = task.getId();
        result.userId = task.getUserId();
        result.courseId = task.getCourseId();
        result.reviewProfileId = task.getReviewProfileId();
        result.teacherProfileId = task.getTeacherProfileId();
        result.providerConfigId = task.getProviderConfigId();
        result.taskType = task.getTaskType();
        result.prompt = task.getPrompt();
        result.status = task.getStatus();
        result.resultPath = task.getResultPath();
        result.errorMessage = task.getErrorMessage();
        result.createTime = task.getCreateTime();
        result.finishTime = task.getFinishTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public Long getReviewProfileId() { return reviewProfileId; }
    public Long getTeacherProfileId() { return teacherProfileId; }
    public Long getProviderConfigId() { return providerConfigId; }
    public String getTaskType() { return taskType; }
    public String getPrompt() { return prompt; }
    public String getStatus() { return status; }
    public String getResultPath() { return resultPath; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
}

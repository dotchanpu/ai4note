package com.example.coursekb.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class MockExamGenerateRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "教师画像 ID 不能为空")
    private Long teacherProfileId;

    @Min(value = 1, message = "题目数量不能小于 1")
    @Max(value = 30, message = "题目数量不能超过 30")
    private Integer questionCount = 8;

    @Size(max = 32, message = "难度不能超过 32 个字符")
    private String difficultyLevel = "MEDIUM";

    @Size(max = 128, message = "模型名称不能超过 128 个字符")
    private String model;

    @Size(max = 1000, message = "补充要求不能超过 1000 个字符")
    private String customRequirement;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTeacherProfileId() { return teacherProfileId; }
    public void setTeacherProfileId(Long teacherProfileId) { this.teacherProfileId = teacherProfileId; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getCustomRequirement() { return customRequirement; }
    public void setCustomRequirement(String customRequirement) { this.customRequirement = customRequirement; }
}

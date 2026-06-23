package com.example.coursekb.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ReviewAssetGenerateRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    private Long teacherProfileId;

    @NotBlank(message = "生成类型不能为空")
    @Size(max = 32, message = "生成类型不能超过 32 个字符")
    private String outputType;

    @Size(max = 32, message = "难度不能超过 32 个字符")
    private String difficultyLevel = "MEDIUM";

    private Boolean includePrerequisites = true;

    @Size(max = 128, message = "模型名称不能超过 128 个字符")
    private String model;

    @Size(max = 1000, message = "补充要求不能超过 1000 个字符")
    private String customRequirement;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTeacherProfileId() { return teacherProfileId; }
    public void setTeacherProfileId(Long teacherProfileId) { this.teacherProfileId = teacherProfileId; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public Boolean getIncludePrerequisites() { return includePrerequisites; }
    public void setIncludePrerequisites(Boolean includePrerequisites) { this.includePrerequisites = includePrerequisites; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getCustomRequirement() { return customRequirement; }
    public void setCustomRequirement(String customRequirement) { this.customRequirement = customRequirement; }
}

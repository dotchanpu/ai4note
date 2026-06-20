package com.example.coursekb.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ReviewGenerationProfileRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "课程 ID 不能为空")
    private Long courseId;

    private Long teacherProfileId;

    @NotBlank(message = "配置名称不能为空")
    @Size(max = 128, message = "配置名称不能超过128个字符")
    private String profileName;

    @Size(max = 128, message = "考试目标不能超过128个字符")
    private String target;

    private String difficultyLevel;
    private String outputType;
    private Boolean includePrerequisites = true;
    private String customRequirement;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getTeacherProfileId() { return teacherProfileId; }
    public void setTeacherProfileId(Long teacherProfileId) { this.teacherProfileId = teacherProfileId; }
    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public Boolean getIncludePrerequisites() { return includePrerequisites; }
    public void setIncludePrerequisites(Boolean includePrerequisites) {
        this.includePrerequisites = includePrerequisites;
    }
    public String getCustomRequirement() { return customRequirement; }
    public void setCustomRequirement(String customRequirement) {
        this.customRequirement = customRequirement;
    }
}

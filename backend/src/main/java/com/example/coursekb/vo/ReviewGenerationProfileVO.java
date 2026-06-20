package com.example.coursekb.vo;

import com.example.coursekb.entity.ReviewGenerationProfile;
import java.time.LocalDateTime;

public class ReviewGenerationProfileVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private Long teacherProfileId;
    private String teacherName;
    private String profileName;
    private String target;
    private String difficultyLevel;
    private String outputType;
    private Boolean includePrerequisites;
    private String customRequirement;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ReviewGenerationProfileVO from(
            ReviewGenerationProfile profile, String teacherName) {
        ReviewGenerationProfileVO result = new ReviewGenerationProfileVO();
        result.id = profile.getId();
        result.userId = profile.getUserId();
        result.courseId = profile.getCourseId();
        result.teacherProfileId = profile.getTeacherProfileId();
        result.teacherName = teacherName;
        result.profileName = profile.getProfileName();
        result.target = profile.getTarget();
        result.difficultyLevel = profile.getDifficultyLevel();
        result.outputType = profile.getOutputType();
        result.includePrerequisites = profile.getIncludePrerequisites();
        result.customRequirement = profile.getCustomRequirement();
        result.createTime = profile.getCreateTime();
        result.updateTime = profile.getUpdateTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public Long getTeacherProfileId() { return teacherProfileId; }
    public String getTeacherName() { return teacherName; }
    public String getProfileName() { return profileName; }
    public String getTarget() { return target; }
    public String getDifficultyLevel() { return difficultyLevel; }
    public String getOutputType() { return outputType; }
    public Boolean getIncludePrerequisites() { return includePrerequisites; }
    public String getCustomRequirement() { return customRequirement; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

package com.example.coursekb.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "review_generation_profile")
public class ReviewGenerationProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "teacher_profile_id")
    private Long teacherProfileId;

    @Column(name = "profile_name", nullable = false, length = 128)
    private String profileName;

    @Column(length = 128)
    private String target;

    @Column(name = "difficulty_level", length = 32)
    private String difficultyLevel;

    @Column(name = "output_type", nullable = false, length = 32)
    private String outputType = "REVIEW_NOTE";

    @Column(name = "include_prerequisites", nullable = false)
    private Byte includePrerequisites = 1;

    @Column(name = "custom_requirement", columnDefinition = "TEXT")
    private String customRequirement;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
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
    public Boolean getIncludePrerequisites() {
        return includePrerequisites != null && includePrerequisites != 0;
    }
    public void setIncludePrerequisites(Boolean includePrerequisites) {
        this.includePrerequisites = Boolean.FALSE.equals(includePrerequisites) ? (byte) 0 : (byte) 1;
    }
    public String getCustomRequirement() { return customRequirement; }
    public void setCustomRequirement(String customRequirement) { this.customRequirement = customRequirement; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

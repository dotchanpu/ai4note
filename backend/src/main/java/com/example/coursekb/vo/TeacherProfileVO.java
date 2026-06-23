package com.example.coursekb.vo;

import com.example.coursekb.entity.TeacherProfile;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TeacherProfileVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private String teacherName;
    private Boolean generatedByAi;
    private String analysisStatus;
    private BigDecimal confidenceScore;
    private String examStyle;
    private String questionPreference;
    private String gradingPreference;
    private String focusTopics;
    private String avoidTopics;
    private String sourceSummary;
    private LocalDateTime lastAnalyzedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static TeacherProfileVO from(TeacherProfile profile) {
        TeacherProfileVO result = new TeacherProfileVO();
        result.id = profile.getId();
        result.userId = profile.getUserId();
        result.courseId = profile.getCourseId();
        result.teacherName = profile.getTeacherName();
        result.generatedByAi = profile.getGeneratedByAi();
        result.analysisStatus = profile.getAnalysisStatus();
        result.confidenceScore = profile.getConfidenceScore();
        result.examStyle = profile.getExamStyle();
        result.questionPreference = profile.getQuestionPreference();
        result.gradingPreference = profile.getGradingPreference();
        result.focusTopics = profile.getFocusTopics();
        result.avoidTopics = profile.getAvoidTopics();
        result.sourceSummary = profile.getSourceSummary();
        result.lastAnalyzedTime = profile.getLastAnalyzedTime();
        result.createTime = profile.getCreateTime();
        result.updateTime = profile.getUpdateTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public String getTeacherName() { return teacherName; }
    public Boolean getGeneratedByAi() { return generatedByAi; }
    public String getAnalysisStatus() { return analysisStatus; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public String getExamStyle() { return examStyle; }
    public String getQuestionPreference() { return questionPreference; }
    public String getGradingPreference() { return gradingPreference; }
    public String getFocusTopics() { return focusTopics; }
    public String getAvoidTopics() { return avoidTopics; }
    public String getSourceSummary() { return sourceSummary; }
    public LocalDateTime getLastAnalyzedTime() { return lastAnalyzedTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

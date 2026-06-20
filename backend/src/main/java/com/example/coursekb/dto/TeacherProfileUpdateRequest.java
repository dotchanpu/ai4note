package com.example.coursekb.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class TeacherProfileUpdateRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @Size(max = 128, message = "教师名称不能超过128个字符")
    private String teacherName;

    private BigDecimal confidenceScore;
    private String examStyle;
    private String questionPreference;
    private String gradingPreference;
    private String focusTopics;
    private String avoidTopics;
    private String sourceSummary;
    private String analysisStatus;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getExamStyle() {
        return examStyle;
    }

    public void setExamStyle(String examStyle) {
        this.examStyle = examStyle;
    }

    public String getQuestionPreference() {
        return questionPreference;
    }

    public void setQuestionPreference(String questionPreference) {
        this.questionPreference = questionPreference;
    }

    public String getGradingPreference() {
        return gradingPreference;
    }

    public void setGradingPreference(String gradingPreference) {
        this.gradingPreference = gradingPreference;
    }

    public String getFocusTopics() {
        return focusTopics;
    }

    public void setFocusTopics(String focusTopics) {
        this.focusTopics = focusTopics;
    }

    public String getAvoidTopics() {
        return avoidTopics;
    }

    public void setAvoidTopics(String avoidTopics) {
        this.avoidTopics = avoidTopics;
    }

    public String getSourceSummary() {
        return sourceSummary;
    }

    public void setSourceSummary(String sourceSummary) {
        this.sourceSummary = sourceSummary;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }
}

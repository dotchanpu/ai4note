package com.example.coursekb.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "teacher_profile")
public class TeacherProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "teacher_name", nullable = false, length = 128)
    private String teacherName;

    @Column(name = "generated_by_ai", nullable = false)
    private Byte generatedByAi = 1;

    @Column(name = "analysis_status", nullable = false, length = 32)
    private String analysisStatus = "PENDING";

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "exam_style", columnDefinition = "TEXT")
    private String examStyle;

    @Column(name = "question_preference", columnDefinition = "TEXT")
    private String questionPreference;

    @Column(name = "grading_preference", columnDefinition = "TEXT")
    private String gradingPreference;

    @Column(name = "focus_topics", columnDefinition = "TEXT")
    private String focusTopics;

    @Column(name = "avoid_topics", columnDefinition = "TEXT")
    private String avoidTopics;

    @Column(name = "source_summary", columnDefinition = "TEXT")
    private String sourceSummary;

    @Column(name = "last_analyzed_time")
    private LocalDateTime lastAnalyzedTime;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public Boolean getGeneratedByAi() { return generatedByAi != null && generatedByAi != 0; }
    public void setGeneratedByAi(Boolean generatedByAi) {
        this.generatedByAi = Boolean.FALSE.equals(generatedByAi) ? (byte) 0 : (byte) 1;
    }
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getExamStyle() { return examStyle; }
    public void setExamStyle(String examStyle) { this.examStyle = examStyle; }
    public String getQuestionPreference() { return questionPreference; }
    public void setQuestionPreference(String questionPreference) { this.questionPreference = questionPreference; }
    public String getGradingPreference() { return gradingPreference; }
    public void setGradingPreference(String gradingPreference) { this.gradingPreference = gradingPreference; }
    public String getFocusTopics() { return focusTopics; }
    public void setFocusTopics(String focusTopics) { this.focusTopics = focusTopics; }
    public String getAvoidTopics() { return avoidTopics; }
    public void setAvoidTopics(String avoidTopics) { this.avoidTopics = avoidTopics; }
    public String getSourceSummary() { return sourceSummary; }
    public void setSourceSummary(String sourceSummary) { this.sourceSummary = sourceSummary; }
    public LocalDateTime getLastAnalyzedTime() { return lastAnalyzedTime; }
    public void setLastAnalyzedTime(LocalDateTime lastAnalyzedTime) { this.lastAnalyzedTime = lastAnalyzedTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

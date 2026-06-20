package com.example.coursekb.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "knowledge_gap_report")
public class KnowledgeGapReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "include_prerequisites", nullable = false)
    private Byte includePrerequisites = 1;

    @Column(name = "report_name", nullable = false, length = 128)
    private String reportName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Boolean getIncludePrerequisites() { return includePrerequisites != null && includePrerequisites != 0; }
    public void setIncludePrerequisites(Boolean includePrerequisites) {
        this.includePrerequisites = Boolean.FALSE.equals(includePrerequisites) ? (byte) 0 : (byte) 1;
    }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getCreateTime() { return createTime; }
}

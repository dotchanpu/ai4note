package com.example.coursekb.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "knowledge_gap_item")
public class KnowledgeGapItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "knowledge_item_id", nullable = false)
    private Long knowledgeItemId;

    @Column(name = "source_course_id", nullable = false)
    private Long sourceCourseId;

    @Column(name = "related_course_relation_id")
    private Long relatedCourseRelationId;

    @Column(name = "gap_type", nullable = false, length = 32)
    private String gapType;

    @Column(name = "severity_level", nullable = false)
    private Integer severityLevel = 1;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Long getKnowledgeItemId() { return knowledgeItemId; }
    public void setKnowledgeItemId(Long knowledgeItemId) { this.knowledgeItemId = knowledgeItemId; }
    public Long getSourceCourseId() { return sourceCourseId; }
    public void setSourceCourseId(Long sourceCourseId) { this.sourceCourseId = sourceCourseId; }
    public Long getRelatedCourseRelationId() { return relatedCourseRelationId; }
    public void setRelatedCourseRelationId(Long relatedCourseRelationId) {
        this.relatedCourseRelationId = relatedCourseRelationId;
    }
    public String getGapType() { return gapType; }
    public void setGapType(String gapType) { this.gapType = gapType; }
    public Integer getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(Integer severityLevel) { this.severityLevel = severityLevel; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public LocalDateTime getCreateTime() { return createTime; }
}

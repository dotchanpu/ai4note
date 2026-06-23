package com.example.coursekb.vo;

import com.example.coursekb.entity.KnowledgeGapItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class KnowledgeGapItemVO {
    private Long id;
    private Long reportId;
    private Long knowledgeItemId;
    private String knowledgeTitle;
    private String knowledgeItemType;
    private Long sourceCourseId;
    private String sourceCourseName;
    private Long relatedCourseRelationId;
    private String relationType;
    private String gapType;
    private Integer severityLevel;
    private String reason;
    private String suggestion;
    private String masteryStatus;
    private BigDecimal masteryScore;
    private long examQuestionCount;
    private BigDecimal examTotalScore;
    private Integer latestExamYear;
    private LocalDateTime createTime;

    public static KnowledgeGapItemVO from(
            KnowledgeGapItem item,
            String knowledgeTitle,
            String knowledgeItemType,
            String sourceCourseName,
            String relationType,
            String masteryStatus,
            BigDecimal masteryScore,
            long examQuestionCount,
            BigDecimal examTotalScore,
            Integer latestExamYear) {
        KnowledgeGapItemVO result = new KnowledgeGapItemVO();
        result.id = item.getId();
        result.reportId = item.getReportId();
        result.knowledgeItemId = item.getKnowledgeItemId();
        result.knowledgeTitle = knowledgeTitle;
        result.knowledgeItemType = knowledgeItemType;
        result.sourceCourseId = item.getSourceCourseId();
        result.sourceCourseName = sourceCourseName;
        result.relatedCourseRelationId = item.getRelatedCourseRelationId();
        result.relationType = relationType;
        result.gapType = item.getGapType();
        result.severityLevel = item.getSeverityLevel();
        result.reason = item.getReason();
        result.suggestion = item.getSuggestion();
        result.masteryStatus = masteryStatus;
        result.masteryScore = masteryScore;
        result.examQuestionCount = examQuestionCount;
        result.examTotalScore = examTotalScore;
        result.latestExamYear = latestExamYear;
        result.createTime = item.getCreateTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getReportId() { return reportId; }
    public Long getKnowledgeItemId() { return knowledgeItemId; }
    public String getKnowledgeTitle() { return knowledgeTitle; }
    public String getKnowledgeItemType() { return knowledgeItemType; }
    public Long getSourceCourseId() { return sourceCourseId; }
    public String getSourceCourseName() { return sourceCourseName; }
    public Long getRelatedCourseRelationId() { return relatedCourseRelationId; }
    public String getRelationType() { return relationType; }
    public String getGapType() { return gapType; }
    public Integer getSeverityLevel() { return severityLevel; }
    public String getReason() { return reason; }
    public String getSuggestion() { return suggestion; }
    public String getMasteryStatus() { return masteryStatus; }
    public BigDecimal getMasteryScore() { return masteryScore; }
    public long getExamQuestionCount() { return examQuestionCount; }
    public BigDecimal getExamTotalScore() { return examTotalScore; }
    public Integer getLatestExamYear() { return latestExamYear; }
    public LocalDateTime getCreateTime() { return createTime; }
}

package com.example.coursekb.vo;

import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamQuestionKnowledgeMapVO {
    private Long id;
    private Long examQuestionId;
    private Long knowledgeItemId;
    private String knowledgeTitle;
    private String knowledgeItemType;
    private String matchSource;
    private BigDecimal confidenceScore;
    private String reason;
    private LocalDateTime createTime;

    public static ExamQuestionKnowledgeMapVO from(
            ExamQuestionKnowledgeMap map, String knowledgeTitle, String knowledgeItemType) {
        ExamQuestionKnowledgeMapVO result = new ExamQuestionKnowledgeMapVO();
        result.id = map.getId();
        result.examQuestionId = map.getExamQuestionId();
        result.knowledgeItemId = map.getKnowledgeItemId();
        result.knowledgeTitle = knowledgeTitle;
        result.knowledgeItemType = knowledgeItemType;
        result.matchSource = map.getMatchSource();
        result.confidenceScore = map.getConfidenceScore();
        result.reason = map.getReason();
        result.createTime = map.getCreateTime();
        return result;
    }

    public Long getId() {
        return id;
    }

    public Long getExamQuestionId() {
        return examQuestionId;
    }

    public Long getKnowledgeItemId() {
        return knowledgeItemId;
    }

    public String getKnowledgeTitle() {
        return knowledgeTitle;
    }

    public String getKnowledgeItemType() {
        return knowledgeItemType;
    }

    public String getMatchSource() {
        return matchSource;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
}

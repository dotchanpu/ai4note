package com.example.coursekb.vo;

import com.example.coursekb.entity.UserKnowledgeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class KnowledgeMasteryVO {
    private Long id;
    private Long userId;
    private Long knowledgeItemId;
    private String masteryStatus;
    private BigDecimal masteryScore;
    private String note;
    private LocalDateTime lastReviewTime;
    private LocalDateTime updateTime;

    public static KnowledgeMasteryVO defaultFor(Long userId, Long knowledgeItemId) {
        KnowledgeMasteryVO result = new KnowledgeMasteryVO();
        result.userId = userId;
        result.knowledgeItemId = knowledgeItemId;
        result.masteryStatus = "UNKNOWN";
        return result;
    }

    public static KnowledgeMasteryVO from(UserKnowledgeStatus status) {
        KnowledgeMasteryVO result = new KnowledgeMasteryVO();
        result.id = status.getId();
        result.userId = status.getUserId();
        result.knowledgeItemId = status.getKnowledgeItemId();
        result.masteryStatus = status.getMasteryStatus();
        result.masteryScore = status.getMasteryScore();
        result.note = status.getNote();
        result.lastReviewTime = status.getLastReviewTime();
        result.updateTime = status.getUpdateTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getKnowledgeItemId() { return knowledgeItemId; }
    public String getMasteryStatus() { return masteryStatus; }
    public BigDecimal getMasteryScore() { return masteryScore; }
    public String getNote() { return note; }
    public LocalDateTime getLastReviewTime() { return lastReviewTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

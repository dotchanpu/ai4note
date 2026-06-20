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
@Table(name = "user_knowledge_status")
public class UserKnowledgeStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "knowledge_item_id", nullable = false)
    private Long knowledgeItemId;

    @Column(name = "mastery_status", nullable = false, length = 32)
    private String masteryStatus = "UNKNOWN";

    @Column(name = "mastery_score")
    private BigDecimal masteryScore;

    @Column(name = "last_review_time")
    private LocalDateTime lastReviewTime;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgeItemId() { return knowledgeItemId; }
    public void setKnowledgeItemId(Long knowledgeItemId) { this.knowledgeItemId = knowledgeItemId; }
    public String getMasteryStatus() { return masteryStatus; }
    public void setMasteryStatus(String masteryStatus) { this.masteryStatus = masteryStatus; }
    public BigDecimal getMasteryScore() { return masteryScore; }
    public void setMasteryScore(BigDecimal masteryScore) { this.masteryScore = masteryScore; }
    public LocalDateTime getLastReviewTime() { return lastReviewTime; }
    public void setLastReviewTime(LocalDateTime lastReviewTime) { this.lastReviewTime = lastReviewTime; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

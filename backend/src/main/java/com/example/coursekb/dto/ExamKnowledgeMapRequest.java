package com.example.coursekb.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ExamKnowledgeMapRequest {
    @NotNull(message = "knowledgeItemId 不能为空")
    private Long knowledgeItemId;

    @Size(max = 32, message = "matchSource 长度不能超过 32")
    private String matchSource;

    @DecimalMin(value = "0.00", inclusive = false, message = "confidenceScore 必须大于 0")
    @DecimalMax(value = "100.00", message = "confidenceScore 不能超过 100")
    private BigDecimal confidenceScore;

    @Size(max = 2000, message = "reason 长度不能超过 2000")
    private String reason;

    public Long getKnowledgeItemId() {
        return knowledgeItemId;
    }

    public void setKnowledgeItemId(Long knowledgeItemId) {
        this.knowledgeItemId = knowledgeItemId;
    }

    public String getMatchSource() {
        return matchSource;
    }

    public void setMatchSource(String matchSource) {
        this.matchSource = matchSource;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

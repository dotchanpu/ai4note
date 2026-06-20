package com.example.coursekb.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExamKnowledgeTrendVO {
    private Long knowledgeItemId;
    private String knowledgeTitle;
    private String knowledgeItemType;
    private String chapterTitle;
    private long totalQuestionCount;
    private BigDecimal totalScore = BigDecimal.ZERO;
    private List<YearStat> yearlyStats = new ArrayList<>();

    public Long getKnowledgeItemId() { return knowledgeItemId; }
    public void setKnowledgeItemId(Long knowledgeItemId) { this.knowledgeItemId = knowledgeItemId; }
    public String getKnowledgeTitle() { return knowledgeTitle; }
    public void setKnowledgeTitle(String knowledgeTitle) { this.knowledgeTitle = knowledgeTitle; }
    public String getKnowledgeItemType() { return knowledgeItemType; }
    public void setKnowledgeItemType(String knowledgeItemType) { this.knowledgeItemType = knowledgeItemType; }
    public String getChapterTitle() { return chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public long getTotalQuestionCount() { return totalQuestionCount; }
    public void setTotalQuestionCount(long totalQuestionCount) { this.totalQuestionCount = totalQuestionCount; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public List<YearStat> getYearlyStats() { return yearlyStats; }
    public void setYearlyStats(List<YearStat> yearlyStats) { this.yearlyStats = yearlyStats; }

    public static class YearStat {
        private Integer examYear;
        private long questionCount;
        private BigDecimal totalScore = BigDecimal.ZERO;

        public YearStat(Integer examYear, long questionCount, BigDecimal totalScore) {
            this.examYear = examYear;
            this.questionCount = questionCount;
            this.totalScore = totalScore == null ? BigDecimal.ZERO : totalScore;
        }

        public Integer getExamYear() { return examYear; }
        public long getQuestionCount() { return questionCount; }
        public BigDecimal getTotalScore() { return totalScore; }
    }
}

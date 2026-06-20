package com.example.coursekb.vo;

import java.math.BigDecimal;

public class ExamKnowledgeStatVO {
    private Long knowledgeItemId;
    private String knowledgeTitle;
    private String knowledgeItemType;
    private String chapterTitle;
    private long questionCount;
    private BigDecimal totalScore;
    private Integer latestExamYear;

    public Long getKnowledgeItemId() {
        return knowledgeItemId;
    }

    public void setKnowledgeItemId(Long knowledgeItemId) {
        this.knowledgeItemId = knowledgeItemId;
    }

    public String getKnowledgeTitle() {
        return knowledgeTitle;
    }

    public void setKnowledgeTitle(String knowledgeTitle) {
        this.knowledgeTitle = knowledgeTitle;
    }

    public String getKnowledgeItemType() {
        return knowledgeItemType;
    }

    public void setKnowledgeItemType(String knowledgeItemType) {
        this.knowledgeItemType = knowledgeItemType;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public long getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(long questionCount) {
        this.questionCount = questionCount;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getLatestExamYear() {
        return latestExamYear;
    }

    public void setLatestExamYear(Integer latestExamYear) {
        this.latestExamYear = latestExamYear;
    }
}

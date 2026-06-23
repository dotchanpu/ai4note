package com.example.coursekb.vo;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeRemediationPathVO {
    private Long reportId;
    private String reportName;
    private String summary;
    private int totalItemCount;
    private List<Stage> stages = new ArrayList<>();

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public int getTotalItemCount() { return totalItemCount; }
    public void setTotalItemCount(int totalItemCount) { this.totalItemCount = totalItemCount; }
    public List<Stage> getStages() { return stages; }
    public void setStages(List<Stage> stages) { this.stages = stages; }

    public static class Stage {
        private String stageKey;
        private String stageName;
        private String objective;
        private List<Item> items = new ArrayList<>();

        public Stage(String stageKey, String stageName, String objective) {
            this.stageKey = stageKey;
            this.stageName = stageName;
            this.objective = objective;
        }

        public String getStageKey() { return stageKey; }
        public String getStageName() { return stageName; }
        public String getObjective() { return objective; }
        public List<Item> getItems() { return items; }
    }

    public static class Item {
        private Long knowledgeItemId;
        private String knowledgeTitle;
        private String sourceCourseName;
        private String gapType;
        private Integer severityLevel;
        private long examQuestionCount;
        private String suggestion;

        public Item(KnowledgeGapItemVO gapItem) {
            this.knowledgeItemId = gapItem.getKnowledgeItemId();
            this.knowledgeTitle = gapItem.getKnowledgeTitle();
            this.sourceCourseName = gapItem.getSourceCourseName();
            this.gapType = gapItem.getGapType();
            this.severityLevel = gapItem.getSeverityLevel();
            this.examQuestionCount = gapItem.getExamQuestionCount();
            this.suggestion = gapItem.getSuggestion();
        }

        public Long getKnowledgeItemId() { return knowledgeItemId; }
        public String getKnowledgeTitle() { return knowledgeTitle; }
        public String getSourceCourseName() { return sourceCourseName; }
        public String getGapType() { return gapType; }
        public Integer getSeverityLevel() { return severityLevel; }
        public long getExamQuestionCount() { return examQuestionCount; }
        public String getSuggestion() { return suggestion; }
    }
}

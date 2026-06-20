package com.example.coursekb.vo;

import java.util.ArrayList;
import java.util.List;

public class CourseStatsVO {
    private Long courseId;
    private String courseName;
    private long materialCount;
    private long parsedMaterialCount;
    private long knowledgeItemCount;
    private long examQuestionCount;
    private long examMappingCount;
    private long exportCount;
    private List<MaterialTypeStat> materialTypeStats = new ArrayList<>();

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public long getMaterialCount() { return materialCount; }
    public void setMaterialCount(long materialCount) { this.materialCount = materialCount; }
    public long getParsedMaterialCount() { return parsedMaterialCount; }
    public void setParsedMaterialCount(long parsedMaterialCount) { this.parsedMaterialCount = parsedMaterialCount; }
    public long getKnowledgeItemCount() { return knowledgeItemCount; }
    public void setKnowledgeItemCount(long knowledgeItemCount) { this.knowledgeItemCount = knowledgeItemCount; }
    public long getExamQuestionCount() { return examQuestionCount; }
    public void setExamQuestionCount(long examQuestionCount) { this.examQuestionCount = examQuestionCount; }
    public long getExamMappingCount() { return examMappingCount; }
    public void setExamMappingCount(long examMappingCount) { this.examMappingCount = examMappingCount; }
    public long getExportCount() { return exportCount; }
    public void setExportCount(long exportCount) { this.exportCount = exportCount; }
    public List<MaterialTypeStat> getMaterialTypeStats() { return materialTypeStats; }
    public void setMaterialTypeStats(List<MaterialTypeStat> materialTypeStats) { this.materialTypeStats = materialTypeStats; }

    public static class MaterialTypeStat {
        private String materialType;
        private long count;

        public MaterialTypeStat(String materialType, long count) {
            this.materialType = materialType;
            this.count = count;
        }

        public String getMaterialType() { return materialType; }
        public void setMaterialType(String materialType) { this.materialType = materialType; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}

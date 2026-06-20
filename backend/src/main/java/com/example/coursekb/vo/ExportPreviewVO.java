package com.example.coursekb.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExportPreviewVO {
    private Long courseId;
    private String courseName;
    private Long templateId;
    private String templateName;
    private String exportName;
    private String exportFormat;
    private String scope;
    private Summary summary = new Summary();
    private List<ChapterPreview> chapters = new ArrayList<>();
    private List<MaterialPreview> materials = new ArrayList<>();
    private List<KnowledgeItemPreview> knowledgeItems = new ArrayList<>();
    private List<ExamStatPreview> examStats = new ArrayList<>();
    private List<RelatedCoursePreview> relatedCourses = new ArrayList<>();

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getExportName() { return exportName; }
    public void setExportName(String exportName) { this.exportName = exportName; }
    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }
    public List<ChapterPreview> getChapters() { return chapters; }
    public void setChapters(List<ChapterPreview> chapters) { this.chapters = chapters; }
    public List<MaterialPreview> getMaterials() { return materials; }
    public void setMaterials(List<MaterialPreview> materials) { this.materials = materials; }
    public List<KnowledgeItemPreview> getKnowledgeItems() { return knowledgeItems; }
    public void setKnowledgeItems(List<KnowledgeItemPreview> knowledgeItems) { this.knowledgeItems = knowledgeItems; }
    public List<ExamStatPreview> getExamStats() { return examStats; }
    public void setExamStats(List<ExamStatPreview> examStats) { this.examStats = examStats; }
    public List<RelatedCoursePreview> getRelatedCourses() { return relatedCourses; }
    public void setRelatedCourses(List<RelatedCoursePreview> relatedCourses) { this.relatedCourses = relatedCourses; }

    public static class Summary {
        private int chapterCount;
        private int materialCount;
        private int parsedMaterialCount;
        private int knowledgeItemCount;
        private int examStatCount;
        private int relatedCourseCount;
        private int relatedMaterialCount;
        private int relatedKnowledgeItemCount;

        public int getChapterCount() { return chapterCount; }
        public void setChapterCount(int chapterCount) { this.chapterCount = chapterCount; }
        public int getMaterialCount() { return materialCount; }
        public void setMaterialCount(int materialCount) { this.materialCount = materialCount; }
        public int getParsedMaterialCount() { return parsedMaterialCount; }
        public void setParsedMaterialCount(int parsedMaterialCount) { this.parsedMaterialCount = parsedMaterialCount; }
        public int getKnowledgeItemCount() { return knowledgeItemCount; }
        public void setKnowledgeItemCount(int knowledgeItemCount) { this.knowledgeItemCount = knowledgeItemCount; }
        public int getExamStatCount() { return examStatCount; }
        public void setExamStatCount(int examStatCount) { this.examStatCount = examStatCount; }
        public int getRelatedCourseCount() { return relatedCourseCount; }
        public void setRelatedCourseCount(int relatedCourseCount) { this.relatedCourseCount = relatedCourseCount; }
        public int getRelatedMaterialCount() { return relatedMaterialCount; }
        public void setRelatedMaterialCount(int relatedMaterialCount) { this.relatedMaterialCount = relatedMaterialCount; }
        public int getRelatedKnowledgeItemCount() { return relatedKnowledgeItemCount; }
        public void setRelatedKnowledgeItemCount(int relatedKnowledgeItemCount) { this.relatedKnowledgeItemCount = relatedKnowledgeItemCount; }
    }

    public static class ChapterPreview {
        private Long id;
        private String chapterNo;
        private String chapterTitle;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getChapterNo() { return chapterNo; }
        public void setChapterNo(String chapterNo) { this.chapterNo = chapterNo; }
        public String getChapterTitle() { return chapterTitle; }
        public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    }

    public static class MaterialPreview {
        private Long id;
        private String title;
        private String materialType;
        private Integer year;
        private Boolean key;
        private Long parsedChunkCount;
        private List<String> tags = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMaterialType() { return materialType; }
        public void setMaterialType(String materialType) { this.materialType = materialType; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Boolean getKey() { return key; }
        public void setKey(Boolean key) { this.key = key; }
        public Long getParsedChunkCount() { return parsedChunkCount; }
        public void setParsedChunkCount(Long parsedChunkCount) { this.parsedChunkCount = parsedChunkCount; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static class KnowledgeItemPreview {
        private Long id;
        private String title;
        private String itemType;
        private Integer importanceLevel;
        private Integer sourcePage;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public Integer getImportanceLevel() { return importanceLevel; }
        public void setImportanceLevel(Integer importanceLevel) { this.importanceLevel = importanceLevel; }
        public Integer getSourcePage() { return sourcePage; }
        public void setSourcePage(Integer sourcePage) { this.sourcePage = sourcePage; }
    }

    public static class ExamStatPreview {
        private Long knowledgeItemId;
        private String knowledgeTitle;
        private String knowledgeItemType;
        private String chapterTitle;
        private long questionCount;
        private BigDecimal totalScore;
        private Integer latestExamYear;

        public Long getKnowledgeItemId() { return knowledgeItemId; }
        public void setKnowledgeItemId(Long knowledgeItemId) { this.knowledgeItemId = knowledgeItemId; }
        public String getKnowledgeTitle() { return knowledgeTitle; }
        public void setKnowledgeTitle(String knowledgeTitle) { this.knowledgeTitle = knowledgeTitle; }
        public String getKnowledgeItemType() { return knowledgeItemType; }
        public void setKnowledgeItemType(String knowledgeItemType) { this.knowledgeItemType = knowledgeItemType; }
        public String getChapterTitle() { return chapterTitle; }
        public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
        public long getQuestionCount() { return questionCount; }
        public void setQuestionCount(long questionCount) { this.questionCount = questionCount; }
        public BigDecimal getTotalScore() { return totalScore; }
        public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
        public Integer getLatestExamYear() { return latestExamYear; }
        public void setLatestExamYear(Integer latestExamYear) { this.latestExamYear = latestExamYear; }
    }

    public static class RelatedCoursePreview {
        private Long courseId;
        private String courseName;
        private String courseCode;
        private String relationType;
        private String relationReason;
        private int chapterCount;
        private List<MaterialPreview> materials = new ArrayList<>();
        private List<KnowledgeItemPreview> knowledgeItems = new ArrayList<>();

        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }
        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }
        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getRelationReason() { return relationReason; }
        public void setRelationReason(String relationReason) { this.relationReason = relationReason; }
        public int getChapterCount() { return chapterCount; }
        public void setChapterCount(int chapterCount) { this.chapterCount = chapterCount; }
        public List<MaterialPreview> getMaterials() { return materials; }
        public void setMaterials(List<MaterialPreview> materials) { this.materials = materials; }
        public List<KnowledgeItemPreview> getKnowledgeItems() { return knowledgeItems; }
        public void setKnowledgeItems(List<KnowledgeItemPreview> knowledgeItems) { this.knowledgeItems = knowledgeItems; }
    }
}

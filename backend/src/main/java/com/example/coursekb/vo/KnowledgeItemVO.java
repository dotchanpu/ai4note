package com.example.coursekb.vo;

import com.example.coursekb.entity.KnowledgeItem;
import java.time.LocalDateTime;

public class KnowledgeItemVO {
    private Long id;
    private Long courseId;
    private Long materialId;
    private Long chapterId;
    private String title;
    private String itemType;
    private String content;
    private Integer sourcePage;
    private Integer importanceLevel;
    private LocalDateTime createTime;
    private String materialTitle;
    private String chapterTitle;

    public static KnowledgeItemVO from(
            KnowledgeItem item, String materialTitle, String chapterTitle) {
        KnowledgeItemVO result = new KnowledgeItemVO();
        result.id = item.getId();
        result.courseId = item.getCourseId();
        result.materialId = item.getMaterialId();
        result.chapterId = item.getChapterId();
        result.title = item.getTitle();
        result.itemType = item.getItemType();
        result.content = item.getContent();
        result.sourcePage = item.getSourcePage();
        result.importanceLevel = item.getImportanceLevel();
        result.createTime = item.getCreateTime();
        result.materialTitle = materialTitle;
        result.chapterTitle = chapterTitle;
        return result;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getMaterialId() { return materialId; }
    public Long getChapterId() { return chapterId; }
    public String getTitle() { return title; }
    public String getItemType() { return itemType; }
    public String getContent() { return content; }
    public Integer getSourcePage() { return sourcePage; }
    public Integer getImportanceLevel() { return importanceLevel; }
    public LocalDateTime getCreateTime() { return createTime; }
    public String getMaterialTitle() { return materialTitle; }
    public String getChapterTitle() { return chapterTitle; }
}

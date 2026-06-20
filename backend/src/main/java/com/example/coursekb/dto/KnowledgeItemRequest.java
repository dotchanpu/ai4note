package com.example.coursekb.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class KnowledgeItemRequest {
    private Long materialId;
    private Long chapterId;

    @NotBlank(message = "知识条目标题不能为空")
    @Size(max = 255, message = "知识条目标题不能超过255个字符")
    private String title;

    @NotBlank(message = "知识条目类型不能为空")
    @Size(max = 32, message = "知识条目类型不能超过32个字符")
    private String itemType;

    @NotBlank(message = "知识条目内容不能为空")
    private String content;

    private Integer sourcePage;

    @Min(value = 1, message = "重要程度不能小于1")
    @Max(value = 5, message = "重要程度不能大于5")
    private Integer importanceLevel = 1;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSourcePage() {
        return sourcePage;
    }

    public void setSourcePage(Integer sourcePage) {
        this.sourcePage = sourcePage;
    }

    public Integer getImportanceLevel() {
        return importanceLevel;
    }

    public void setImportanceLevel(Integer importanceLevel) {
        this.importanceLevel = importanceLevel;
    }
}

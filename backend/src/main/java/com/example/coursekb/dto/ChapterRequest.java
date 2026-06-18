package com.example.coursekb.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ChapterRequest {
    @NotBlank(message = "章节编号不能为空")
    @Size(max = 64, message = "章节编号不能超过64个字符")
    private String chapterNo;

    @NotBlank(message = "章节标题不能为空")
    @Size(max = 128, message = "章节标题不能超过128个字符")
    private String chapterTitle;

    private Integer sortOrder = 0;

    public String getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(String chapterNo) {
        this.chapterNo = chapterNo;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}

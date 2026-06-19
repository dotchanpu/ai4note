package com.example.coursekb.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class MaterialUpdateRequest {
    private Long chapterId;

    @NotBlank(message = "资料标题不能为空")
    @Size(max = 255, message = "资料标题不能超过255个字符")
    private String title;

    @NotBlank(message = "资料类型不能为空")
    @Size(max = 32, message = "资料类型不能超过32个字符")
    private String materialType;

    private Integer year;
    private Boolean key;
    private String summary;

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

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Boolean getKey() {
        return key;
    }

    public void setKey(Boolean key) {
        this.key = key;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}

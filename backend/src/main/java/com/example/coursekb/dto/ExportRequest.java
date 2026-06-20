package com.example.coursekb.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ExportRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "课程 ID 不能为空")
    private Long courseId;

    private Long templateId;

    @NotBlank(message = "导出名称不能为空")
    @Size(max = 255, message = "导出名称不能超过255个字符")
    private String exportName;

    private String exportFormat = "ZIP";
    private List<Long> chapterIds;
    private List<String> materialTypes;
    private Boolean onlyKeyMaterials;
    private Boolean includeExamStats;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public List<Long> getChapterIds() {
        return chapterIds;
    }

    public void setChapterIds(List<Long> chapterIds) {
        this.chapterIds = chapterIds;
    }

    public List<String> getMaterialTypes() {
        return materialTypes;
    }

    public void setMaterialTypes(List<String> materialTypes) {
        this.materialTypes = materialTypes;
    }

    public Boolean getOnlyKeyMaterials() {
        return onlyKeyMaterials;
    }

    public void setOnlyKeyMaterials(Boolean onlyKeyMaterials) {
        this.onlyKeyMaterials = onlyKeyMaterials;
    }

    public Boolean getIncludeExamStats() {
        return includeExamStats;
    }

    public void setIncludeExamStats(Boolean includeExamStats) {
        this.includeExamStats = includeExamStats;
    }
}

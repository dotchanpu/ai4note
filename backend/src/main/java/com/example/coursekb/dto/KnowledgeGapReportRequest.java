package com.example.coursekb.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class KnowledgeGapReportRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    private Boolean includePrerequisites = true;

    @Size(max = 128, message = "报告名称不能超过128个字符")
    private String reportName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getIncludePrerequisites() {
        return includePrerequisites;
    }

    public void setIncludePrerequisites(Boolean includePrerequisites) {
        this.includePrerequisites = includePrerequisites;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }
}

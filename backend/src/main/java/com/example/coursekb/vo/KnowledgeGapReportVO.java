package com.example.coursekb.vo;

import com.example.coursekb.entity.KnowledgeGapReport;
import java.time.LocalDateTime;

public class KnowledgeGapReportVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private String courseName;
    private Boolean includePrerequisites;
    private String reportName;
    private String summary;
    private long itemCount;
    private LocalDateTime createTime;

    public static KnowledgeGapReportVO from(
            KnowledgeGapReport report, String courseName, long itemCount) {
        KnowledgeGapReportVO result = new KnowledgeGapReportVO();
        result.id = report.getId();
        result.userId = report.getUserId();
        result.courseId = report.getCourseId();
        result.courseName = courseName;
        result.includePrerequisites = report.getIncludePrerequisites();
        result.reportName = report.getReportName();
        result.summary = report.getSummary();
        result.itemCount = itemCount;
        result.createTime = report.getCreateTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public Boolean getIncludePrerequisites() { return includePrerequisites; }
    public String getReportName() { return reportName; }
    public String getSummary() { return summary; }
    public long getItemCount() { return itemCount; }
    public LocalDateTime getCreateTime() { return createTime; }
}

package com.example.coursekb.vo;

import com.example.coursekb.entity.ExportRecord;
import java.time.LocalDateTime;

public class ExportRecordVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private Long templateId;
    private String exportName;
    private String exportFormat;
    private String exportPath;
    private String exportScope;
    private LocalDateTime exportTime;

    public static ExportRecordVO from(ExportRecord record) {
        ExportRecordVO result = new ExportRecordVO();
        result.id = record.getId();
        result.userId = record.getUserId();
        result.courseId = record.getCourseId();
        result.templateId = record.getTemplateId();
        result.exportName = record.getExportName();
        result.exportFormat = record.getExportFormat();
        result.exportPath = record.getExportPath();
        result.exportScope = record.getExportScope();
        result.exportTime = record.getExportTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public Long getTemplateId() { return templateId; }
    public String getExportName() { return exportName; }
    public String getExportFormat() { return exportFormat; }
    public String getExportPath() { return exportPath; }
    public String getExportScope() { return exportScope; }
    public LocalDateTime getExportTime() { return exportTime; }
}

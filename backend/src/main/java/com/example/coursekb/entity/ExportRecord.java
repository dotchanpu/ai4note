package com.example.coursekb.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "export_record")
public class ExportRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "export_name", nullable = false, length = 255)
    private String exportName;

    @Column(name = "export_format", nullable = false, length = 32)
    private String exportFormat;

    @Column(name = "export_path", nullable = false, length = 512)
    private String exportPath;

    @Column(name = "export_scope", columnDefinition = "TEXT")
    private String exportScope;

    @Column(name = "export_time", insertable = false, updatable = false)
    private LocalDateTime exportTime;

    public Long getId() {
        return id;
    }

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

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public String getExportScope() {
        return exportScope;
    }

    public void setExportScope(String exportScope) {
        this.exportScope = exportScope;
    }

    public LocalDateTime getExportTime() {
        return exportTime;
    }
}

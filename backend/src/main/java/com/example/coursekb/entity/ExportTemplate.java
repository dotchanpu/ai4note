package com.example.coursekb.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "export_template")
public class ExportTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    @Column(name = "target_agent", nullable = false, length = 64)
    private String targetAgent;

    @Column(name = "template_format", nullable = false, length = 32)
    private String templateFormat;

    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    private String templateContent;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTargetAgent() {
        return targetAgent;
    }

    public String getTemplateFormat() {
        return templateFormat;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
}

package com.example.coursekb.vo;

import com.example.coursekb.entity.ExportTemplate;
import java.time.LocalDateTime;

public class ExportTemplateVO {
    private Long id;
    private String templateName;
    private String targetAgent;
    private String templateFormat;
    private String templateContent;
    private LocalDateTime createTime;

    public static ExportTemplateVO from(ExportTemplate template) {
        ExportTemplateVO result = new ExportTemplateVO();
        result.id = template.getId();
        result.templateName = template.getTemplateName();
        result.targetAgent = template.getTargetAgent();
        result.templateFormat = template.getTemplateFormat();
        result.templateContent = template.getTemplateContent();
        result.createTime = template.getCreateTime();
        return result;
    }

    public Long getId() { return id; }
    public String getTemplateName() { return templateName; }
    public String getTargetAgent() { return targetAgent; }
    public String getTemplateFormat() { return templateFormat; }
    public String getTemplateContent() { return templateContent; }
    public LocalDateTime getCreateTime() { return createTime; }
}

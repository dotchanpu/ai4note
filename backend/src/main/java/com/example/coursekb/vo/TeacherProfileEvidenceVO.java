package com.example.coursekb.vo;

import com.example.coursekb.entity.TeacherProfileEvidence;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TeacherProfileEvidenceVO {
    private Long id;
    private Long teacherProfileId;
    private Long materialId;
    private String materialTitle;
    private String materialType;
    private String evidenceType;
    private String evidenceSummary;
    private Integer sourcePage;
    private BigDecimal confidenceScore;
    private LocalDateTime createTime;

    public static TeacherProfileEvidenceVO from(
            TeacherProfileEvidence evidence, String materialTitle, String materialType) {
        TeacherProfileEvidenceVO result = new TeacherProfileEvidenceVO();
        result.id = evidence.getId();
        result.teacherProfileId = evidence.getTeacherProfileId();
        result.materialId = evidence.getMaterialId();
        result.materialTitle = materialTitle;
        result.materialType = materialType;
        result.evidenceType = evidence.getEvidenceType();
        result.evidenceSummary = evidence.getEvidenceSummary();
        result.sourcePage = evidence.getSourcePage();
        result.confidenceScore = evidence.getConfidenceScore();
        result.createTime = evidence.getCreateTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getTeacherProfileId() { return teacherProfileId; }
    public Long getMaterialId() { return materialId; }
    public String getMaterialTitle() { return materialTitle; }
    public String getMaterialType() { return materialType; }
    public String getEvidenceType() { return evidenceType; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public Integer getSourcePage() { return sourcePage; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public LocalDateTime getCreateTime() { return createTime; }
}

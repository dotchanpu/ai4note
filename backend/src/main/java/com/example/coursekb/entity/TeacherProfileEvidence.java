package com.example.coursekb.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "teacher_profile_evidence")
public class TeacherProfileEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_profile_id", nullable = false)
    private Long teacherProfileId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "evidence_type", nullable = false, length = 32)
    private String evidenceType;

    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(name = "source_page")
    private Integer sourcePage;

    @Column(name = "confidence_score")
    private BigDecimal confidenceScore;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public Long getTeacherProfileId() { return teacherProfileId; }
    public void setTeacherProfileId(Long teacherProfileId) { this.teacherProfileId = teacherProfileId; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public String getEvidenceSummary() { return evidenceSummary; }
    public void setEvidenceSummary(String evidenceSummary) { this.evidenceSummary = evidenceSummary; }
    public Integer getSourcePage() { return sourcePage; }
    public void setSourcePage(Integer sourcePage) { this.sourcePage = sourcePage; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public LocalDateTime getCreateTime() { return createTime; }
}

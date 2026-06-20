package com.example.coursekb.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "course_relation")
public class CourseRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "prerequisite_course_id", nullable = false)
    private Long relatedCourseId;

    @Column(name = "relation_type", nullable = false, length = 32)
    private String relationType = "PREREQUISITE";

    @Column(length = 255)
    private String reason;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getRelatedCourseId() { return relatedCourseId; }
    public void setRelatedCourseId(Long relatedCourseId) { this.relatedCourseId = relatedCourseId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
}

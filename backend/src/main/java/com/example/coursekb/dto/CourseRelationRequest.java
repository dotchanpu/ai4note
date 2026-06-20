package com.example.coursekb.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CourseRelationRequest {
    @NotNull(message = "关联课程不能为空")
    private Long relatedCourseId;

    @Size(max = 32, message = "关系类型不能超过32个字符")
    private String relationType;

    @Size(max = 255, message = "关系说明不能超过255个字符")
    private String reason;

    private Integer sortOrder;

    public Long getRelatedCourseId() { return relatedCourseId; }
    public void setRelatedCourseId(Long relatedCourseId) { this.relatedCourseId = relatedCourseId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}

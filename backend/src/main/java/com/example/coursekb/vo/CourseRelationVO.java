package com.example.coursekb.vo;

import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.CourseRelation;
import java.time.LocalDateTime;

public class CourseRelationVO {
    private Long id;
    private Long courseId;
    private Long relatedCourseId;
    private String relationType;
    private String reason;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private String relatedCourseName;
    private String relatedCourseCode;
    private String relatedSemester;
    private String relatedDescription;

    public static CourseRelationVO from(CourseRelation relation, Course relatedCourse) {
        CourseRelationVO result = new CourseRelationVO();
        result.id = relation.getId();
        result.courseId = relation.getCourseId();
        result.relatedCourseId = relation.getRelatedCourseId();
        result.relationType = relation.getRelationType();
        result.reason = relation.getReason();
        result.sortOrder = relation.getSortOrder();
        result.createTime = relation.getCreateTime();
        if (relatedCourse != null) {
            result.relatedCourseName = relatedCourse.getCourseName();
            result.relatedCourseCode = relatedCourse.getCourseCode();
            result.relatedSemester = relatedCourse.getSemester();
            result.relatedDescription = relatedCourse.getDescription();
        }
        return result;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getRelatedCourseId() { return relatedCourseId; }
    public String getRelationType() { return relationType; }
    public String getReason() { return reason; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public String getRelatedCourseName() { return relatedCourseName; }
    public String getRelatedCourseCode() { return relatedCourseCode; }
    public String getRelatedSemester() { return relatedSemester; }
    public String getRelatedDescription() { return relatedDescription; }
}

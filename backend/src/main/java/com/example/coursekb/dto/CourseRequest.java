package com.example.coursekb.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CourseRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 128, message = "课程名称不能超过128个字符")
    private String courseName;

    @Size(max = 64, message = "课程编号不能超过64个字符")
    private String courseCode;

    @Size(max = 64, message = "学期不能超过64个字符")
    private String semester;

    private String description;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

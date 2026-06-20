package com.example.coursekb.vo;

import java.time.LocalDateTime;

public class SearchRecordVO {
    private Long id;
    private Long userId;
    private Long courseId;
    private String courseName;
    private String keyword;
    private String searchType;
    private Integer resultCount;
    private LocalDateTime searchTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
    public Integer getResultCount() { return resultCount; }
    public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
    public LocalDateTime getSearchTime() { return searchTime; }
    public void setSearchTime(LocalDateTime searchTime) { this.searchTime = searchTime; }
}

package com.example.coursekb.vo;

import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.MaterialFile;
import java.time.LocalDateTime;

public class MaterialVO {
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String materialType;
    private String summary;
    private Integer year;
    private Boolean key;
    private LocalDateTime uploadTime;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private Long parsedChunkCount;

    public static MaterialVO from(Material material, MaterialFile file, long parsedChunkCount) {
        MaterialVO result = new MaterialVO();
        result.id = material.getId();
        result.courseId = material.getCourseId();
        result.chapterId = material.getChapterId();
        result.title = material.getTitle();
        result.materialType = material.getMaterialType();
        result.summary = material.getSummary();
        result.year = material.getYear();
        result.key = material.getKey();
        result.uploadTime = material.getUploadTime();
        result.parsedChunkCount = parsedChunkCount;
        if (file != null) {
            result.originalName = file.getOriginalName();
            result.fileType = file.getFileType();
            result.fileSize = file.getFileSize();
        }
        return result;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public String getTitle() {
        return title;
    }

    public String getMaterialType() {
        return materialType;
    }

    public String getSummary() {
        return summary;
    }

    public Integer getYear() {
        return year;
    }

    public Boolean getKey() {
        return key;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getFileType() {
        return fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Long getParsedChunkCount() {
        return parsedChunkCount;
    }
}

package com.example.coursekb.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AiChatRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "提问内容不能为空")
    @Size(max = 12000, message = "提问内容不能超过12000个字符")
    private String message;

    private String model;

    private Boolean thinking = false;

    private Integer maxTokens = 2048;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getThinking() {
        return thinking;
    }

    public void setThinking(Boolean thinking) {
        this.thinking = thinking;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}

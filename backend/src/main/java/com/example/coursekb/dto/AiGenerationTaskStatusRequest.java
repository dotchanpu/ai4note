package com.example.coursekb.dto;

import javax.validation.constraints.NotNull;

public class AiGenerationTaskStatusRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    private String status;
    private String resultPath;
    private String errorMessage;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultPath() { return resultPath; }
    public void setResultPath(String resultPath) { this.resultPath = resultPath; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

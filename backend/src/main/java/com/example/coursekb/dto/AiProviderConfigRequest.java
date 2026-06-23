package com.example.coursekb.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AiProviderConfigRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 64, message = "供应商名称不能超过64个字符")
    private String providerName;

    @NotBlank(message = "Base URL 不能为空")
    @Size(max = 255, message = "Base URL 不能超过255个字符")
    private String baseUrl;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "模型名称不能超过128个字符")
    private String modelName;

    @Size(max = 128, message = "API Key 环境变量别名不能超过128个字符")
    private String apiKeyAlias;

    private Boolean enabled = true;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getApiKeyAlias() { return apiKeyAlias; }
    public void setApiKeyAlias(String apiKeyAlias) { this.apiKeyAlias = apiKeyAlias; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

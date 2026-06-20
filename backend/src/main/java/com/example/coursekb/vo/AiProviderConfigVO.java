package com.example.coursekb.vo;

import com.example.coursekb.entity.AiProviderConfig;
import java.time.LocalDateTime;

public class AiProviderConfigVO {
    private Long id;
    private Long userId;
    private String providerName;
    private String baseUrl;
    private String modelName;
    private String apiKeyAlias;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static AiProviderConfigVO from(AiProviderConfig config) {
        AiProviderConfigVO result = new AiProviderConfigVO();
        result.id = config.getId();
        result.userId = config.getUserId();
        result.providerName = config.getProviderName();
        result.baseUrl = config.getBaseUrl();
        result.modelName = config.getModelName();
        result.apiKeyAlias = config.getApiKeyAlias();
        result.enabled = config.getEnabled();
        result.createTime = config.getCreateTime();
        result.updateTime = config.getUpdateTime();
        return result;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getProviderName() { return providerName; }
    public String getBaseUrl() { return baseUrl; }
    public String getModelName() { return modelName; }
    public String getApiKeyAlias() { return apiKeyAlias; }
    public Boolean getEnabled() { return enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

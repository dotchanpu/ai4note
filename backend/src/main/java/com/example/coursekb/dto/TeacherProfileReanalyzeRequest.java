package com.example.coursekb.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class TeacherProfileReanalyzeRequest {
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    private List<Long> materialIds;

    @Size(max = 128, message = "模型名称不能超过 128 个字符")
    private String model;

    private Long providerConfigId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<Long> getMaterialIds() { return materialIds; }
    public void setMaterialIds(List<Long> materialIds) { this.materialIds = materialIds; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
}

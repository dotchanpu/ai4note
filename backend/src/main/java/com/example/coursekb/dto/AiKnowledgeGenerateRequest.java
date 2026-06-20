package com.example.coursekb.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class AiKnowledgeGenerateRequest {
    @Min(value = 1, message = "生成数量不能小于1")
    @Max(value = 30, message = "生成数量不能超过30")
    private Integer maxItems = 12;

    private Boolean replaceExisting = false;
    private String model;

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    public Boolean getReplaceExisting() {
        return replaceExisting;
    }

    public void setReplaceExisting(Boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

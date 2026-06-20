package com.example.coursekb.vo;

import java.util.List;

public class AiKnowledgeGenerateResultVO {
    private List<String> tags;
    private List<KnowledgeItemVO> items;

    public AiKnowledgeGenerateResultVO(List<String> tags, List<KnowledgeItemVO> items) {
        this.tags = tags;
        this.items = items;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<KnowledgeItemVO> getItems() {
        return items;
    }
}

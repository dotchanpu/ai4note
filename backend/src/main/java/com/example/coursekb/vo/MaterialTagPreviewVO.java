package com.example.coursekb.vo;

import java.util.List;

public class MaterialTagPreviewVO {
    private Long materialId;
    private List<String> tags;

    public MaterialTagPreviewVO(Long materialId, List<String> tags) {
        this.materialId = materialId;
        this.tags = tags;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public List<String> getTags() {
        return tags;
    }
}

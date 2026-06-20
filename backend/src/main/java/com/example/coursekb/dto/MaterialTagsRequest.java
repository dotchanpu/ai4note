package com.example.coursekb.dto;

import java.util.ArrayList;
import java.util.List;

public class MaterialTagsRequest {
    private List<String> tagNames = new ArrayList<>();

    public List<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames;
    }
}

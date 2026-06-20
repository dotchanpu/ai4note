package com.example.coursekb.vo;

import java.util.ArrayList;
import java.util.List;

public class MaterialSimilarityVO {
    private MaterialVO material;
    private double score;
    private List<String> reasons = new ArrayList<>();

    public MaterialSimilarityVO(MaterialVO material, double score, List<String> reasons) {
        this.material = material;
        this.score = score;
        this.reasons = reasons;
    }

    public MaterialVO getMaterial() {
        return material;
    }

    public double getScore() {
        return score;
    }

    public List<String> getReasons() {
        return reasons;
    }
}

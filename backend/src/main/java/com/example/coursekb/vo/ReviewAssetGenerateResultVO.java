package com.example.coursekb.vo;

public class ReviewAssetGenerateResultVO {
    private AiGenerationTaskVO task;
    private String title;
    private String outputType;
    private String resultPath;
    private String content;
    private MaterialVO material;

    public ReviewAssetGenerateResultVO(
            AiGenerationTaskVO task,
            String title,
            String outputType,
            String resultPath,
            String content,
            MaterialVO material) {
        this.task = task;
        this.title = title;
        this.outputType = outputType;
        this.resultPath = resultPath;
        this.content = content;
        this.material = material;
    }

    public AiGenerationTaskVO getTask() { return task; }
    public String getTitle() { return title; }
    public String getOutputType() { return outputType; }
    public String getResultPath() { return resultPath; }
    public String getContent() { return content; }
    public MaterialVO getMaterial() { return material; }
}

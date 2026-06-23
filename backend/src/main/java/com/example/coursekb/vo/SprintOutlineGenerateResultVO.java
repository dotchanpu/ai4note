package com.example.coursekb.vo;

public class SprintOutlineGenerateResultVO {
    private AiGenerationTaskVO task;
    private String title;
    private String resultPath;
    private String content;
    private int dayCount;

    public SprintOutlineGenerateResultVO(
            AiGenerationTaskVO task,
            String title,
            String resultPath,
            String content,
            int dayCount) {
        this.task = task;
        this.title = title;
        this.resultPath = resultPath;
        this.content = content;
        this.dayCount = dayCount;
    }

    public AiGenerationTaskVO getTask() { return task; }
    public String getTitle() { return title; }
    public String getResultPath() { return resultPath; }
    public String getContent() { return content; }
    public int getDayCount() { return dayCount; }
}

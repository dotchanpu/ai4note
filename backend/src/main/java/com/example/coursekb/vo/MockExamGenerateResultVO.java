package com.example.coursekb.vo;

public class MockExamGenerateResultVO {
    private AiGenerationTaskVO task;
    private String title;
    private String resultPath;
    private String content;
    private int questionCount;

    public MockExamGenerateResultVO(
            AiGenerationTaskVO task,
            String title,
            String resultPath,
            String content,
            int questionCount) {
        this.task = task;
        this.title = title;
        this.resultPath = resultPath;
        this.content = content;
        this.questionCount = questionCount;
    }

    public AiGenerationTaskVO getTask() { return task; }
    public String getTitle() { return title; }
    public String getResultPath() { return resultPath; }
    public String getContent() { return content; }
    public int getQuestionCount() { return questionCount; }
}

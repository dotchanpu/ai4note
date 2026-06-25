package com.example.coursekb.vo;

import com.example.coursekb.entity.ExamQuestion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ExamQuestionVO {
    private Long id;
    private Long courseId;
    private Long materialId;
    private Long chapterId;
    private String questionNo;
    private String questionType;
    private String questionText;
    private String answerText;
    private String difficultyLevel;
    private BigDecimal score;
    private Integer examYear;
    private Integer sourcePage;
    private LocalDateTime createTime;
    private String materialTitle;
    private String chapterTitle;
    private String answerSource;
    private Integer answerSourcePage;
    private List<ExamQuestionKnowledgeMapVO> mappings;

    public static ExamQuestionVO from(
            ExamQuestion question,
            String questionNo,
            String questionType,
            String materialTitle,
            String chapterTitle,
            List<ExamQuestionKnowledgeMapVO> mappings) {
        ExamQuestionVO result = new ExamQuestionVO();
        result.id = question.getId();
        result.courseId = question.getCourseId();
        result.materialId = question.getMaterialId();
        result.chapterId = question.getChapterId();
        result.questionNo = questionNo;
        result.questionType = questionType;
        result.questionText = question.getQuestionText();
        result.answerText = question.getAnswerText();
        result.difficultyLevel = question.getDifficultyLevel();
        result.score = question.getScore();
        result.examYear = question.getExamYear();
        result.sourcePage = question.getSourcePage();
        result.createTime = question.getCreateTime();
        result.materialTitle = materialTitle;
        result.chapterTitle = chapterTitle;
        result.answerSource = question.getAnswerSource();
        result.answerSourcePage = question.getAnswerSourcePage();
        result.mappings = mappings == null ? Collections.emptyList() : mappings;
        return result;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public String getQuestionNo() {
        return questionNo;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswerText() {
        return answerText;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Integer getExamYear() {
        return examYear;
    }

    public Integer getSourcePage() {
        return sourcePage;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public String getMaterialTitle() {
        return materialTitle;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public String getAnswerSource() {
        return answerSource;
    }

    public Integer getAnswerSourcePage() {
        return answerSourcePage;
    }

    public List<ExamQuestionKnowledgeMapVO> getMappings() {
        return mappings;
    }
}

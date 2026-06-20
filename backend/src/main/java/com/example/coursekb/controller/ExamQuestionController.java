package com.example.coursekb.controller;

import com.example.coursekb.dto.ExamKnowledgeMapRequest;
import com.example.coursekb.service.ExamQuestionService;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExamKnowledgeTrendVO;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionPageVO;
import com.example.coursekb.vo.ExamQuestionVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExamQuestionController {
    private final ExamQuestionService examQuestionService;

    public ExamQuestionController(ExamQuestionService examQuestionService) {
        this.examQuestionService = examQuestionService;
    }

    @PostMapping("/materials/{materialId}/exam-questions/extract")
    public List<ExamQuestionVO> extractQuestions(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean overwrite) {
        return examQuestionService.extractQuestions(materialId, userId, overwrite);
    }

    @GetMapping("/courses/{courseId}/exam-questions")
    public ExamQuestionPageVO listQuestions(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Long materialId) {
        return examQuestionService.listQuestions(
                courseId, userId, year, chapterId, questionType, materialId, page, size);
    }

    @PostMapping("/exam-questions/{questionId}/knowledge-map")
    public ExamQuestionKnowledgeMapVO saveKnowledgeMap(
            @PathVariable Long questionId,
            @RequestParam Long userId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ExamKnowledgeMapRequest request) {
        return examQuestionService.saveKnowledgeMap(questionId, userId, request);
    }

    @GetMapping("/courses/{courseId}/exam-knowledge-stats")
    public List<ExamKnowledgeStatVO> listKnowledgeStats(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String questionType) {
        return examQuestionService.listKnowledgeStats(
                courseId, userId, year, chapterId, questionType);
    }

    @GetMapping("/courses/{courseId}/exam-knowledge-trends")
    public List<ExamKnowledgeTrendVO> listKnowledgeTrends(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String questionType) {
        return examQuestionService.listKnowledgeTrends(courseId, userId, chapterId, questionType);
    }
}

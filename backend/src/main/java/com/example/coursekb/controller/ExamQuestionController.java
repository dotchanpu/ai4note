package com.example.coursekb.controller;

import com.example.coursekb.dto.ExamKnowledgeMapRequest;
import com.example.coursekb.service.ExamQuestionService;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExamKnowledgeTrendVO;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionPageVO;
import com.example.coursekb.vo.ExamQuestionVO;
import java.util.List;
import java.util.Map;
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
            @RequestParam(name = "chapterIds", required = false) List<Long> chapterIds,
            @RequestParam(required = false) String questionType,
            @RequestParam(name = "materialIds", required = false) List<Long> materialIds) {
        return examQuestionService.listQuestions(
                courseId, userId, year, chapterIds, questionType, materialIds, page, size);
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
            @RequestParam(name = "chapterIds", required = false) List<Long> chapterIds,
            @RequestParam(required = false) String questionType,
            @RequestParam(name = "materialIds", required = false) List<Long> materialIds) {
        return examQuestionService.listKnowledgeStats(
                courseId, userId, year, chapterIds, questionType, materialIds);
    }

    @GetMapping("/courses/{courseId}/exam-knowledge-trends")
    public List<ExamKnowledgeTrendVO> listKnowledgeTrends(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @RequestParam(name = "chapterIds", required = false) List<Long> chapterIds,
            @RequestParam(required = false) String questionType,
            @RequestParam(name = "materialIds", required = false) List<Long> materialIds) {
        return examQuestionService.listKnowledgeTrends(courseId, userId, chapterIds, questionType, materialIds);
    }

    @PostMapping("/exam-questions/{questionId}/generate-answer")
    public ExamQuestionVO generateAnswer(
            @PathVariable Long questionId,
            @RequestParam Long userId) {
        return examQuestionService.generateAnswer(questionId, userId);
    }

    @PostMapping("/courses/{courseId}/exam-questions/generate-answers")
    public Map<String, Object> generateBatchAnswers(
            @PathVariable Long courseId,
            @RequestParam Long userId) {
        return examQuestionService.generateBatchAnswers(courseId, userId);
    }
}

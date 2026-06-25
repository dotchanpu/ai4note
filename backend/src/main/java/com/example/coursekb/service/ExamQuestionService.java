package com.example.coursekb.service;

import com.example.coursekb.dto.ExamKnowledgeMapRequest;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.ExamQuestionKnowledgeMapRepository;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExamKnowledgeTrendVO;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionPageVO;
import com.example.coursekb.vo.ExamQuestionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 真题模块的外部门面服务。
 * 保持对外 API 稳定，同时把具体职责分发给内部更细的服务组件。
 */
@Service
public class ExamQuestionService {
    private final ExamQuestionCommandService examQuestionCommandService;
    private final ExamQuestionQueryService examQuestionQueryService;
    private final ExamQuestionAnswerService examQuestionAnswerService;

    @Autowired
    public ExamQuestionService(
            ExamQuestionCommandService examQuestionCommandService,
            ExamQuestionQueryService examQuestionQueryService,
            ExamQuestionAnswerService examQuestionAnswerService) {
        this.examQuestionCommandService = examQuestionCommandService;
        this.examQuestionQueryService = examQuestionQueryService;
        this.examQuestionAnswerService = examQuestionAnswerService;
    }

    ExamQuestionService(
            MaterialService materialService,
            CourseService courseService,
            TextChunkRepository textChunkRepository,
            ExamQuestionRepository examQuestionRepository,
            ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            MaterialFileRepository materialFileRepository,
            MaterialRepository materialRepository,
            ChapterRepository chapterRepository,
            DeepSeekService deepSeekService,
            ObjectMapper objectMapper) {
        ExamQuestionNormalizer examQuestionNormalizer = new ExamQuestionNormalizer();
        ExamQuestionAssembler examQuestionAssembler = new ExamQuestionAssembler(
                examQuestionKnowledgeMapRepository,
                knowledgeItemRepository,
                materialRepository,
                chapterRepository,
                examQuestionNormalizer);
        ExamExtractionAiService examExtractionAiService =
                new ExamExtractionAiService(deepSeekService, objectMapper, examQuestionNormalizer);
        ExamAutoMapAiService examAutoMapAiService = new ExamAutoMapAiService(
                deepSeekService,
                objectMapper,
                examQuestionKnowledgeMapRepository,
                knowledgeItemRepository,
                chapterRepository,
                examQuestionNormalizer);
        this.examQuestionCommandService = new ExamQuestionCommandService(
                materialService,
                courseService,
                examQuestionRepository,
                examQuestionKnowledgeMapRepository,
                knowledgeItemRepository,
                materialFileRepository,
                textChunkRepository,
                examExtractionAiService,
                examAutoMapAiService,
                examQuestionAssembler,
                examQuestionNormalizer);
        this.examQuestionQueryService = new ExamQuestionQueryService(
                courseService,
                examQuestionRepository,
                examQuestionKnowledgeMapRepository,
                knowledgeItemRepository,
                chapterRepository,
                examQuestionAssembler,
                examQuestionNormalizer);
        this.examQuestionAnswerService = new ExamQuestionAnswerService(
                courseService,
                examQuestionRepository,
                materialRepository,
                textChunkRepository,
                deepSeekService,
                objectMapper,
                examQuestionAssembler,
                examQuestionNormalizer);
    }

    public List<ExamQuestionVO> extractQuestions(Long materialId, Long userId, boolean overwrite) {
        return examQuestionCommandService.extractQuestions(materialId, userId, overwrite);
    }

    public ExamQuestionPageVO listQuestions(
            Long courseId,
            Long userId,
            Integer year,
            List<Long> chapterIds,
            String questionType,
            List<Long> materialIds,
            Integer page,
            Integer size) {
        return examQuestionQueryService.listQuestions(
                courseId, userId, year, chapterIds, questionType, materialIds, page, size);
    }

    public ExamQuestionKnowledgeMapVO saveKnowledgeMap(
            Long questionId, Long userId, ExamKnowledgeMapRequest request) {
        return examQuestionCommandService.saveKnowledgeMap(questionId, userId, request);
    }

    public List<ExamKnowledgeStatVO> listKnowledgeStats(
            Long courseId,
            Long userId,
            Integer year,
            List<Long> chapterIds,
            String questionType,
            List<Long> materialIds) {
        return examQuestionQueryService.listKnowledgeStats(
                courseId, userId, year, chapterIds, questionType, materialIds);
    }

    public List<ExamKnowledgeTrendVO> listKnowledgeTrends(
            Long courseId,
            Long userId,
            List<Long> chapterIds,
            String questionType,
            List<Long> materialIds) {
        return examQuestionQueryService.listKnowledgeTrends(
                courseId, userId, chapterIds, questionType, materialIds);
    }

    public ExamQuestionVO generateAnswer(Long questionId, Long userId) {
        return examQuestionAnswerService.generateAnswer(questionId, userId);
    }

    public Map<String, Object> generateBatchAnswers(Long courseId, Long userId) {
        return examQuestionAnswerService.generateBatchAnswers(courseId, userId);
    }
}

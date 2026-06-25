package com.example.coursekb.service;

import com.example.coursekb.dto.ExamKnowledgeMapRequest;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.MaterialFile;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ExamQuestionKnowledgeMapRepository;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionVO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 负责真题写入类流程，例如抽题、覆盖重写以及显式知识点映射更新。
 */
@Service
public class ExamQuestionCommandService {
    public static final String CODE_NOT_PARSED = "MATERIAL_NOT_PARSED";
    public static final String CODE_ALREADY_EXTRACTED = "EXAM_EXTRACTION_EXISTS";
    public static final String CODE_PDF_ONLY = "EXAM_PDF_ONLY";

    private final MaterialService materialService;
    private final CourseService courseService;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final MaterialFileRepository materialFileRepository;
    private final TextChunkRepository textChunkRepository;
    private final ExamExtractionAiService examExtractionAiService;
    private final ExamAutoMapAiService examAutoMapAiService;
    private final ExamQuestionAssembler examQuestionAssembler;
    private final ExamQuestionNormalizer examQuestionNormalizer;

    public ExamQuestionCommandService(
            MaterialService materialService,
            CourseService courseService,
            ExamQuestionRepository examQuestionRepository,
            ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            MaterialFileRepository materialFileRepository,
            TextChunkRepository textChunkRepository,
            ExamExtractionAiService examExtractionAiService,
            ExamAutoMapAiService examAutoMapAiService,
            ExamQuestionAssembler examQuestionAssembler,
            ExamQuestionNormalizer examQuestionNormalizer) {
        this.materialService = materialService;
        this.courseService = courseService;
        this.examQuestionRepository = examQuestionRepository;
        this.examQuestionKnowledgeMapRepository = examQuestionKnowledgeMapRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.materialFileRepository = materialFileRepository;
        this.textChunkRepository = textChunkRepository;
        this.examExtractionAiService = examExtractionAiService;
        this.examAutoMapAiService = examAutoMapAiService;
        this.examQuestionAssembler = examQuestionAssembler;
        this.examQuestionNormalizer = examQuestionNormalizer;
    }

    /**
     * 抽题流程顺序固定为：校验资料 -> AI 解析文本块 -> 按需覆盖旧数据 -> 保存题目 ->
     * 基于现有知识条目做自动映射。
     */
    @Transactional
    public List<ExamQuestionVO> extractQuestions(Long materialId, Long userId, boolean overwrite) {
        Material material = materialService.getOwnedMaterial(materialId, userId);
        ensureExamMaterial(material);

        List<TextChunk> chunks = textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(materialId);
        if (chunks.isEmpty()) {
            throw new BusinessException(CODE_NOT_PARSED, "Material must be parsed before exam extraction");
        }

        long existingCount = examQuestionRepository.countByMaterialId(materialId);
        if (existingCount > 0 && !overwrite) {
            throw new BusinessException(CODE_ALREADY_EXTRACTED, "Exam questions already exist for this material");
        }

        List<ExamExtractionAiService.ExtractedQuestionPayload> extracted =
                examExtractionAiService.extractWithAi(material, userId, chunks);
        if (extracted.isEmpty()) {
            throw new BusinessException("No valid exam questions were extracted");
        }

        if (existingCount > 0) {
            replaceMaterialQuestions(materialId);
        }

        List<ExamQuestion> saved = examQuestionRepository.saveAll(extracted.stream()
                .map(payload -> toExamQuestion(material, payload))
                .collect(Collectors.toList()));
        examAutoMapAiService.autoMapQuestions(material, userId, saved);
        return examQuestionAssembler.toQuestionVOs(saved);
    }

    @Transactional
    public ExamQuestionKnowledgeMapVO saveKnowledgeMap(
            Long questionId, Long userId, ExamKnowledgeMapRequest request) {
        ExamQuestion question = examQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("Exam question does not exist"));
        courseService.getOwnedCourse(question.getCourseId(), userId);

        KnowledgeItem knowledgeItem = knowledgeItemRepository.findByIdAndCourseId(
                        request.getKnowledgeItemId(), question.getCourseId())
                .orElseThrow(() -> new BusinessException("Knowledge item does not exist in this course"));

        ExamQuestionKnowledgeMap map = examQuestionKnowledgeMapRepository
                .findByExamQuestionIdAndKnowledgeItemId(questionId, knowledgeItem.getId())
                .orElseGet(ExamQuestionKnowledgeMap::new);
        map.setExamQuestionId(questionId);
        map.setKnowledgeItemId(knowledgeItem.getId());
        map.setMatchSource(examQuestionNormalizer.normalizeMatchSource(request.getMatchSource()));
        map.setConfidenceScore(request.getConfidenceScore());
        map.setReason(examQuestionNormalizer.normalizeOptionalText(request.getReason()));

        ExamQuestionKnowledgeMap saved = examQuestionKnowledgeMapRepository.save(map);
        return ExamQuestionKnowledgeMapVO.from(saved, knowledgeItem.getTitle(), knowledgeItem.getItemType());
    }

    private static final java.util.Set<String> SUPPORTED_EXTRACTION_FORMATS =
            java.util.Collections.unmodifiableSet(new java.util.HashSet<>(
                    java.util.Arrays.asList("pdf", "doc", "docx", "md", "txt")));

    private void ensureExamMaterial(Material material) {
        if (!"EXAM".equalsIgnoreCase(material.getMaterialType())) {
            throw new BusinessException("Only EXAM materials support exam extraction");
        }
        MaterialFile file = materialFileRepository.findByMaterialId(material.getId())
                .orElseThrow(() -> new BusinessException("Material file does not exist"));
        if (!SUPPORTED_EXTRACTION_FORMATS.contains(file.getFileType().toLowerCase())) {
            throw new BusinessException(CODE_PDF_ONLY,
                    "Unsupported file format for exam extraction; supported: PDF, DOC, DOCX, MD, TXT");
        }
    }

    /**
     * 必须先删映射再删题目，避免留下孤儿映射污染后续统计结果。
     */
    private void replaceMaterialQuestions(Long materialId) {
        List<Long> questionIds = examQuestionRepository.findByMaterialIdOrderByIdAsc(materialId)
                .stream()
                .map(ExamQuestion::getId)
                .collect(Collectors.toList());
        if (!questionIds.isEmpty()) {
            examQuestionKnowledgeMapRepository.deleteByExamQuestionIdIn(questionIds);
        }
        examQuestionRepository.deleteAll(examQuestionRepository.findByMaterialIdOrderByIdAsc(materialId));
    }

    private ExamQuestion toExamQuestion(
            Material material, ExamExtractionAiService.ExtractedQuestionPayload payload) {
        ExamQuestion question = new ExamQuestion();
        question.setCourseId(material.getCourseId());
        question.setMaterialId(material.getId());
        question.setChapterId(material.getChapterId());
        question.setQuestionNo(examQuestionNormalizer.normalizeQuestionNo(payload.getQuestionNo()));
        question.setQuestionType(examQuestionNormalizer.normalizeQuestionType(payload.getQuestionType()));
        question.setQuestionText(payload.getQuestionText());
        question.setAnswerText(payload.getAnswerText());
        question.setDifficultyLevel(payload.getDifficultyLevel());
        question.setScore(payload.getScore());
        question.setExamYear(payload.getExamYear() == null ? material.getYear() : payload.getExamYear());
        question.setSourcePage(payload.getSourcePage());
        return question;
    }
}

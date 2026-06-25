package com.example.coursekb.service;

import com.example.coursekb.entity.Chapter;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.ExamQuestionKnowledgeMapRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionVO;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 负责组装真题展示对象，把展示层补充信息从命令和查询流程中拆出来。
 */
@Component
public class ExamQuestionAssembler {
    private final ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final MaterialRepository materialRepository;
    private final ChapterRepository chapterRepository;
    private final ExamQuestionNormalizer examQuestionNormalizer;

    public ExamQuestionAssembler(
            ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            MaterialRepository materialRepository,
            ChapterRepository chapterRepository,
            ExamQuestionNormalizer examQuestionNormalizer) {
        this.examQuestionKnowledgeMapRepository = examQuestionKnowledgeMapRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.materialRepository = materialRepository;
        this.chapterRepository = chapterRepository;
        this.examQuestionNormalizer = examQuestionNormalizer;
    }

    public List<ExamQuestionVO> toQuestionVOs(List<ExamQuestion> questions) {
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<Long> questionIds = questions.stream().map(ExamQuestion::getId).collect(Collectors.toList());
        List<ExamQuestionKnowledgeMap> mappings =
                examQuestionKnowledgeMapRepository.findByExamQuestionIdInOrderByIdAsc(questionIds);
        Map<Long, KnowledgeItem> knowledgeById = knowledgeItemRepository.findAllById(
                        mappings.stream()
                                .map(ExamQuestionKnowledgeMap::getKnowledgeItemId)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, Function.identity()));

        Map<Long, String> materialTitleById = materialRepository.findAllById(
                        questions.stream()
                                .map(ExamQuestion::getMaterialId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Material::getId, Material::getTitle));
        Map<Long, String> chapterTitleById = chapterRepository.findAllById(
                        questions.stream()
                                .map(ExamQuestion::getChapterId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getChapterTitle));

        Map<Long, List<ExamQuestionKnowledgeMapVO>> mappingsByQuestionId = mappings.stream()
                .collect(Collectors.groupingBy(
                        ExamQuestionKnowledgeMap::getExamQuestionId,
                        LinkedHashMap::new,
                        Collectors.mapping(map -> {
                            KnowledgeItem knowledgeItem = knowledgeById.get(map.getKnowledgeItemId());
                            return ExamQuestionKnowledgeMapVO.from(
                                    map,
                                    knowledgeItem == null ? null : knowledgeItem.getTitle(),
                                    knowledgeItem == null ? null : knowledgeItem.getItemType());
                        }, Collectors.toList())));

        return questions.stream()
                .map(question -> ExamQuestionVO.from(
                        question,
                        examQuestionNormalizer.normalizeQuestionNo(question.getQuestionNo()),
                        examQuestionNormalizer.normalizeQuestionType(question.getQuestionType()),
                        question.getMaterialId() == null ? null : materialTitleById.get(question.getMaterialId()),
                        question.getChapterId() == null ? null : chapterTitleById.get(question.getChapterId()),
                        mappingsByQuestionId.get(question.getId())))
                .collect(Collectors.toList());
    }
}

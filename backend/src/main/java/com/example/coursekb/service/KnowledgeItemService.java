package com.example.coursekb.service;

import com.example.coursekb.dto.KnowledgeItemRequest;
import com.example.coursekb.entity.Chapter;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.vo.KnowledgeItemVO;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeItemService {
    private static final Set<String> ITEM_TYPES = new HashSet<>(Arrays.asList(
            "DEFINITION", "KEY_POINT", "FORMULA", "METHOD", "EXAMPLE", "WARNING"));

    private final CourseService courseService;
    private final MaterialService materialService;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final MaterialRepository materialRepository;
    private final ChapterRepository chapterRepository;
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeItemService(
            CourseService courseService,
            MaterialService materialService,
            KnowledgeItemRepository knowledgeItemRepository,
            MaterialRepository materialRepository,
            ChapterRepository chapterRepository,
            JdbcTemplate jdbcTemplate) {
        this.courseService = courseService;
        this.materialService = materialService;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.materialRepository = materialRepository;
        this.chapterRepository = chapterRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KnowledgeItemVO> list(
            Long courseId, Long userId, Long materialId, Long chapterId, String itemType) {
        courseService.getOwnedCourse(courseId, userId);
        String normalizedType = normalizeOptionalType(itemType);
        return knowledgeItemRepository.findByCourseIdOrderByImportanceLevelDescIdDesc(courseId)
                .stream()
                .filter(item -> materialId == null || materialId.equals(item.getMaterialId()))
                .filter(item -> chapterId == null || chapterId.equals(item.getChapterId()))
                .filter(item -> normalizedType == null || normalizedType.equals(item.getItemType()))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Transactional
    public KnowledgeItemVO create(
            Long courseId, Long userId, KnowledgeItemRequest request) {
        courseService.getOwnedCourse(courseId, userId);
        KnowledgeItem item = new KnowledgeItem();
        apply(item, courseId, userId, request);
        return toVO(knowledgeItemRepository.save(item));
    }

    @Transactional
    public KnowledgeItemVO update(
            Long courseId, Long itemId, Long userId, KnowledgeItemRequest request) {
        courseService.getOwnedCourse(courseId, userId);
        KnowledgeItem item = knowledgeItemRepository.findByIdAndCourseId(itemId, courseId)
                .orElseThrow(() -> new BusinessException("知识条目不存在"));
        apply(item, courseId, userId, request);
        return toVO(knowledgeItemRepository.save(item));
    }

    @Transactional
    public void delete(Long courseId, Long itemId, Long userId) {
        courseService.getOwnedCourse(courseId, userId);
        KnowledgeItem item = knowledgeItemRepository.findByIdAndCourseId(itemId, courseId)
                .orElseThrow(() -> new BusinessException("知识条目不存在"));
        deleteKnowledgeDependencies(item.getId());
        knowledgeItemRepository.delete(item);
    }

    @Transactional
    public List<KnowledgeItemVO> replaceMaterialItems(
            Material material, List<KnowledgeItem> generatedItems, boolean replaceExisting) {
        if (replaceExisting) {
            List<Long> ids = jdbcTemplate.queryForList(
                    "SELECT id FROM knowledge_item WHERE material_id = ?",
                    Long.class,
                    material.getId());
            for (Long id : ids) {
                deleteKnowledgeDependencies(id);
            }
            jdbcTemplate.update(
                    "DELETE FROM knowledge_item WHERE material_id = ?",
                    material.getId());
        }
        return knowledgeItemRepository.saveAll(generatedItems).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private void deleteKnowledgeDependencies(Long itemId) {
        jdbcTemplate.update(
                "DELETE FROM exam_question_knowledge_map WHERE knowledge_item_id = ?",
                itemId);
        jdbcTemplate.update(
                "DELETE FROM knowledge_gap_item WHERE knowledge_item_id = ?",
                itemId);
        jdbcTemplate.update(
                "DELETE FROM user_knowledge_status WHERE knowledge_item_id = ?",
                itemId);
    }

    private void apply(
            KnowledgeItem item,
            Long courseId,
            Long userId,
            KnowledgeItemRequest request) {
        Material material = null;
        if (request.getMaterialId() != null) {
            material = materialService.getOwnedMaterial(request.getMaterialId(), userId);
            if (!courseId.equals(material.getCourseId())) {
                throw new BusinessException("资料不属于当前课程");
            }
        }
        Long chapterId = request.getChapterId();
        if (chapterId == null && material != null) {
            chapterId = material.getChapterId();
        }
        if (chapterId != null && !chapterRepository.findByIdAndCourseId(chapterId, courseId).isPresent()) {
            throw new BusinessException("章节不属于当前课程");
        }

        item.setCourseId(courseId);
        item.setMaterialId(request.getMaterialId());
        item.setChapterId(chapterId);
        item.setTitle(requireText(request.getTitle(), "知识条目标题不能为空"));
        item.setItemType(normalizeType(request.getItemType()));
        item.setContent(requireText(request.getContent(), "知识条目内容不能为空"));
        item.setSourcePage(request.getSourcePage());
        item.setImportanceLevel(request.getImportanceLevel() == null ? 1 : request.getImportanceLevel());
    }

    private KnowledgeItemVO toVO(KnowledgeItem item) {
        String materialTitle = item.getMaterialId() == null
                ? null
                : materialRepository.findById(item.getMaterialId()).map(Material::getTitle).orElse(null);
        String chapterTitle = item.getChapterId() == null
                ? null
                : chapterRepository.findById(item.getChapterId()).map(Chapter::getChapterTitle).orElse(null);
        return KnowledgeItemVO.from(item, materialTitle, chapterTitle);
    }

    private String normalizeType(String value) {
        String normalized = requireText(value, "知识条目类型不能为空").toUpperCase(Locale.ROOT);
        if (!ITEM_TYPES.contains(normalized)) {
            throw new BusinessException("不支持的知识条目类型：" + normalized);
        }
        return normalized;
    }

    private String normalizeOptionalType(String value) {
        return value == null || value.trim().isEmpty() ? null : normalizeType(value);
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}

package com.example.coursekb.service;

import com.example.coursekb.dto.MaterialTagsRequest;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.Tag;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.TagRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
    private static final int MAX_TAGS_PER_MATERIAL = 20;

    private final CourseService courseService;
    private final MaterialService materialService;
    private final TagRepository tagRepository;
    private final JdbcTemplate jdbcTemplate;

    public TagService(
            CourseService courseService,
            MaterialService materialService,
            TagRepository tagRepository,
            JdbcTemplate jdbcTemplate) {
        this.courseService = courseService;
        this.materialService = materialService;
        this.tagRepository = tagRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> listCourseTags(Long courseId, Long userId) {
        courseService.getOwnedCourse(courseId, userId);
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT t.tag_name FROM tag t "
                        + "JOIN material_tag mt ON mt.tag_id = t.id "
                        + "JOIN material m ON m.id = mt.material_id "
                        + "WHERE m.course_id = ? ORDER BY t.tag_name",
                String.class,
                courseId);
    }

    public List<String> listMaterialTags(Long materialId, Long userId) {
        materialService.getOwnedMaterial(materialId, userId);
        return jdbcTemplate.queryForList(
                "SELECT t.tag_name FROM tag t "
                        + "JOIN material_tag mt ON mt.tag_id = t.id "
                        + "WHERE mt.material_id = ? ORDER BY t.tag_name",
                String.class,
                materialId);
    }

    @Transactional
    public List<String> replaceMaterialTags(
            Long materialId, Long userId, MaterialTagsRequest request) {
        Material material = materialService.getOwnedMaterial(materialId, userId);
        return replaceMaterialTagsInternal(material, request == null ? null : request.getTagNames());
    }

    @Transactional
    public List<String> replaceMaterialTagsInternal(Material material, List<String> tagNames) {
        List<String> normalizedTags = normalizeTags(tagNames);
        jdbcTemplate.update("DELETE FROM material_tag WHERE material_id = ?", material.getId());
        for (String tagName : normalizedTags) {
            Tag tag = tagRepository.findByTagName(tagName).orElseGet(() -> {
                Tag created = new Tag();
                created.setTagName(tagName);
                return tagRepository.save(created);
            });
            jdbcTemplate.update(
                    "INSERT INTO material_tag (material_id, tag_id) VALUES (?, ?)",
                    material.getId(),
                    tag.getId());
        }
        return normalizedTags;
    }

    private List<String> normalizeTags(List<String> tagNames) {
        Set<String> unique = new LinkedHashSet<>();
        if (tagNames != null) {
            for (String value : tagNames) {
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }
                String normalized = value.trim().replaceAll("\\s+", " ");
                if (normalized.length() > 64) {
                    throw new BusinessException("标签名称不能超过64个字符");
                }
                unique.add(normalized);
            }
        }
        if (unique.size() > MAX_TAGS_PER_MATERIAL) {
            throw new BusinessException("单份资料最多绑定20个标签");
        }
        return new ArrayList<>(unique);
    }
}

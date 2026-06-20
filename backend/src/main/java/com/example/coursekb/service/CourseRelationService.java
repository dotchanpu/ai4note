package com.example.coursekb.service;

import com.example.coursekb.dto.CourseRelationRequest;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.CourseRelation;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.CourseRelationRepository;
import com.example.coursekb.mapper.CourseRepository;
import com.example.coursekb.vo.CourseRelationVO;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseRelationService {
    private static final Set<String> RELATION_TYPES = new HashSet<>(Arrays.asList(
            "PREREQUISITE", "RELATED", "FOLLOW_UP"));

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseRelationRepository courseRelationRepository;

    public CourseRelationService(
            CourseService courseService,
            CourseRepository courseRepository,
            CourseRelationRepository courseRelationRepository) {
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.courseRelationRepository = courseRelationRepository;
    }

    public List<CourseRelationVO> list(Long courseId, Long userId) {
        courseService.getOwnedCourse(courseId, userId);
        return courseRelationRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseRelationVO create(Long courseId, Long userId, CourseRelationRequest request) {
        courseService.getOwnedCourse(courseId, userId);
        Course relatedCourse = courseService.getOwnedCourse(request.getRelatedCourseId(), userId);
        if (courseId.equals(relatedCourse.getId())) {
            throw new BusinessException("课程不能关联自身");
        }
        if (courseRelationRepository.existsByCourseIdAndRelatedCourseId(courseId, relatedCourse.getId())) {
            throw new BusinessException("当前课程已存在该关联课程");
        }

        CourseRelation relation = new CourseRelation();
        relation.setCourseId(courseId);
        relation.setRelatedCourseId(relatedCourse.getId());
        relation.setRelationType(normalizeRelationType(request.getRelationType()));
        relation.setReason(normalize(request.getReason()));
        relation.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return CourseRelationVO.from(courseRelationRepository.save(relation), relatedCourse);
    }

    @Transactional
    public void delete(Long relationId, Long userId) {
        CourseRelation relation = courseRelationRepository.findById(relationId)
                .orElseThrow(() -> new BusinessException("课程关系不存在"));
        courseService.getOwnedCourse(relation.getCourseId(), userId);
        courseRelationRepository.delete(relation);
    }

    private CourseRelationVO toVO(CourseRelation relation) {
        Course relatedCourse = courseRepository.findById(relation.getRelatedCourseId()).orElse(null);
        return CourseRelationVO.from(relation, relatedCourse);
    }

    private String normalizeRelationType(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "PREREQUISITE"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!RELATION_TYPES.contains(normalized)) {
            throw new BusinessException("不支持的课程关系类型：" + normalized);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

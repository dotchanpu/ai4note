package com.example.coursekb.mapper;

import com.example.coursekb.entity.CourseRelation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRelationRepository extends JpaRepository<CourseRelation, Long> {
    List<CourseRelation> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

    List<CourseRelation> findByCourseIdInOrderByCourseIdAscSortOrderAscIdAsc(List<Long> courseIds);

    Optional<CourseRelation> findByIdAndCourseId(Long id, Long courseId);

    boolean existsByCourseIdAndRelatedCourseId(Long courseId, Long relatedCourseId);
}

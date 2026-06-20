package com.example.coursekb.mapper;

import com.example.coursekb.entity.KnowledgeItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {
    List<KnowledgeItem> findByCourseIdOrderByImportanceLevelDescIdDesc(Long courseId);

    Optional<KnowledgeItem> findByIdAndCourseId(Long id, Long courseId);

    void deleteByMaterialId(Long materialId);
}

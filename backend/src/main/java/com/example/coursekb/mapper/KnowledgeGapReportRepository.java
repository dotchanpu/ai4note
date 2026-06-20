package com.example.coursekb.mapper;

import com.example.coursekb.entity.KnowledgeGapReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeGapReportRepository extends JpaRepository<KnowledgeGapReport, Long> {
    List<KnowledgeGapReport> findByUserIdAndCourseIdOrderByCreateTimeDescIdDesc(
            Long userId, Long courseId);

    Optional<KnowledgeGapReport> findByIdAndUserId(Long id, Long userId);
}

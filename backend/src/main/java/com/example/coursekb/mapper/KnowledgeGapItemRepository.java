package com.example.coursekb.mapper;

import com.example.coursekb.entity.KnowledgeGapItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeGapItemRepository extends JpaRepository<KnowledgeGapItem, Long> {
    List<KnowledgeGapItem> findByReportIdOrderBySeverityLevelDescIdAsc(Long reportId);

    long countByReportId(Long reportId);
}

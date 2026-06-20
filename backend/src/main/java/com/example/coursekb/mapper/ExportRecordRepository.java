package com.example.coursekb.mapper;

import com.example.coursekb.entity.ExportRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportRecordRepository extends JpaRepository<ExportRecord, Long> {
    List<ExportRecord> findByUserIdOrderByExportTimeDescIdDesc(Long userId);

    List<ExportRecord> findByUserIdAndCourseIdOrderByExportTimeDescIdDesc(Long userId, Long courseId);

    Optional<ExportRecord> findByIdAndUserId(Long id, Long userId);
}

package com.example.coursekb.mapper;

import com.example.coursekb.entity.ExportRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExportRecordRepository extends JpaRepository<ExportRecord, Long> {
    List<ExportRecord> findByUserIdOrderByExportTimeDescIdDesc(Long userId);

    List<ExportRecord> findByUserIdAndCourseIdOrderByExportTimeDescIdDesc(Long userId, Long courseId);

    Optional<ExportRecord> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(max(record.versionNo), 0) from ExportRecord record "
            + "where record.userId = :userId and record.courseId = :courseId")
    Integer findMaxVersionNo(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Modifying
    @Query("update ExportRecord record set record.recommendedFlag = 0 "
            + "where record.userId = :userId and record.courseId = :courseId")
    int clearRecommended(@Param("userId") Long userId, @Param("courseId") Long courseId);
}

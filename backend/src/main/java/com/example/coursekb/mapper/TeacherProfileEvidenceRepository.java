package com.example.coursekb.mapper;

import com.example.coursekb.entity.TeacherProfileEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileEvidenceRepository extends JpaRepository<TeacherProfileEvidence, Long> {
    List<TeacherProfileEvidence> findByTeacherProfileIdOrderByConfidenceScoreDescIdAsc(Long teacherProfileId);

    void deleteByTeacherProfileId(Long teacherProfileId);
}

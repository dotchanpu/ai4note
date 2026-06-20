package com.example.coursekb.mapper;

import com.example.coursekb.entity.AiGenerationTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationTaskRepository extends JpaRepository<AiGenerationTask, Long> {
    List<AiGenerationTask> findByUserIdAndCourseIdOrderByCreateTimeDescIdDesc(Long userId, Long courseId);

    List<AiGenerationTask> findByUserIdOrderByCreateTimeDescIdDesc(Long userId);

    Optional<AiGenerationTask> findByIdAndUserId(Long id, Long userId);
}

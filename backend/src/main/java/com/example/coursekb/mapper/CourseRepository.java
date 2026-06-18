package com.example.coursekb.mapper;

import com.example.coursekb.entity.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserIdOrderByCreateTimeDesc(Long userId);

    Optional<Course> findByIdAndUserId(Long id, Long userId);
}

package com.example.coursekb.mapper;

import com.example.coursekb.entity.TeacherProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    List<TeacherProfile> findByUserIdAndCourseIdOrderByUpdateTimeDescIdDesc(Long userId, Long courseId);

    Optional<TeacherProfile> findByIdAndUserId(Long id, Long userId);
}

package com.example.coursekb.mapper;

import com.example.coursekb.entity.ReviewGenerationProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewGenerationProfileRepository extends JpaRepository<ReviewGenerationProfile, Long> {
    List<ReviewGenerationProfile> findByUserIdAndCourseIdOrderByUpdateTimeDescIdDesc(Long userId, Long courseId);

    Optional<ReviewGenerationProfile> findByIdAndUserId(Long id, Long userId);
}

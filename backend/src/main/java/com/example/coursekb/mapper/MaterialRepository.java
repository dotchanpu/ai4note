package com.example.coursekb.mapper;

import com.example.coursekb.entity.Material;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByCourseIdOrderByUploadTimeDesc(Long courseId);

    Optional<Material> findByIdAndCourseId(Long id, Long courseId);

    List<Material> findByCourseIdAndMaterialTypeInOrderByUploadTimeDesc(Long courseId, List<String> materialTypes);
}

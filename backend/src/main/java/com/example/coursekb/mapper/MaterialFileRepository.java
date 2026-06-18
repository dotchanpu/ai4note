package com.example.coursekb.mapper;

import com.example.coursekb.entity.MaterialFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialFileRepository extends JpaRepository<MaterialFile, Long> {
    Optional<MaterialFile> findByMaterialId(Long materialId);
}

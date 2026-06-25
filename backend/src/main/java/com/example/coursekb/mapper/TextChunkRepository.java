package com.example.coursekb.mapper;

import com.example.coursekb.entity.TextChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextChunkRepository extends JpaRepository<TextChunk, Long> {
    List<TextChunk> findByMaterialIdOrderByChunkIndexAsc(Long materialId);

    long countByMaterialId(Long materialId);

    void deleteByMaterialId(Long materialId);

    List<TextChunk> findByMaterialIdInOrderByPageNoAscChunkIndexAsc(List<Long> materialIds);
}

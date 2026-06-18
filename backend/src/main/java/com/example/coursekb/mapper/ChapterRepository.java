package com.example.coursekb.mapper;

import com.example.coursekb.entity.Chapter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

    boolean existsByCourseIdAndChapterNo(Long courseId, String chapterNo);
}

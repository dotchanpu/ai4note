package com.example.coursekb.service;

import com.example.coursekb.dto.ChapterRequest;
import com.example.coursekb.entity.Chapter;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ChapterRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final CourseService courseService;

    public ChapterService(ChapterRepository chapterRepository, CourseService courseService) {
        this.chapterRepository = chapterRepository;
        this.courseService = courseService;
    }

    public List<Chapter> list(Long courseId, Long userId) {
        courseService.getOwnedCourse(courseId, userId);
        return chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId);
    }

    @Transactional
    public Chapter create(Long courseId, Long userId, ChapterRequest request) {
        courseService.getOwnedCourse(courseId, userId);
        String chapterNo = request.getChapterNo().trim();
        if (chapterRepository.existsByCourseIdAndChapterNo(courseId, chapterNo)) {
            throw new BusinessException("当前课程中已存在相同章节编号");
        }

        Chapter chapter = new Chapter();
        chapter.setCourseId(courseId);
        chapter.setChapterNo(chapterNo);
        chapter.setChapterTitle(request.getChapterTitle().trim());
        chapter.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        return chapterRepository.save(chapter);
    }
}

package com.example.coursekb.controller;

import com.example.coursekb.dto.ChapterRequest;
import com.example.coursekb.entity.Chapter;
import com.example.coursekb.service.ChapterService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/{courseId}/chapters")
public class ChapterController {
    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    public List<Chapter> list(@PathVariable Long courseId, @RequestParam Long userId) {
        return chapterService.list(courseId, userId);
    }

    @PostMapping
    public Chapter create(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @Valid @RequestBody ChapterRequest request) {
        return chapterService.create(courseId, userId, request);
    }

    @PutMapping("/{chapterId}")
    public Chapter update(
            @PathVariable Long courseId,
            @PathVariable Long chapterId,
            @RequestParam Long userId,
            @Valid @RequestBody ChapterRequest request) {
        return chapterService.update(courseId, chapterId, userId, request);
    }
}

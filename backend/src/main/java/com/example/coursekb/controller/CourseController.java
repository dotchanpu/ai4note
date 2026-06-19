package com.example.coursekb.controller;

import com.example.coursekb.dto.CourseRequest;
import com.example.coursekb.entity.Course;
import com.example.coursekb.service.ContentDeletionService;
import com.example.coursekb.service.CourseService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService courseService;
    private final ContentDeletionService contentDeletionService;

    public CourseController(
            CourseService courseService,
            ContentDeletionService contentDeletionService) {
        this.courseService = courseService;
        this.contentDeletionService = contentDeletionService;
    }

    @GetMapping
    public List<Course> list(@RequestParam Long userId) {
        return courseService.listByUser(userId);
    }

    @GetMapping("/{courseId}")
    public Course detail(@PathVariable Long courseId, @RequestParam Long userId) {
        return courseService.getOwnedCourse(courseId, userId);
    }

    @PostMapping
    public Course create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    @PutMapping("/{courseId}")
    public Course update(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseRequest request) {
        return courseService.update(courseId, request);
    }

    @DeleteMapping("/{courseId}")
    public void delete(@PathVariable Long courseId, @RequestParam Long userId) {
        contentDeletionService.deleteCourse(courseId, userId);
    }
}

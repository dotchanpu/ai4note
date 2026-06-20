package com.example.coursekb.controller;

import com.example.coursekb.dto.CourseRelationRequest;
import com.example.coursekb.service.CourseRelationService;
import com.example.coursekb.vo.CourseRelationVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CourseRelationController {
    private final CourseRelationService courseRelationService;

    public CourseRelationController(CourseRelationService courseRelationService) {
        this.courseRelationService = courseRelationService;
    }

    @GetMapping("/courses/{courseId}/relations")
    public List<CourseRelationVO> list(@PathVariable Long courseId, @RequestParam Long userId) {
        return courseRelationService.list(courseId, userId);
    }

    @PostMapping("/courses/{courseId}/relations")
    public CourseRelationVO create(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @Valid @RequestBody CourseRelationRequest request) {
        return courseRelationService.create(courseId, userId, request);
    }

    @DeleteMapping("/course-relations/{relationId}")
    public void delete(@PathVariable Long relationId, @RequestParam Long userId) {
        courseRelationService.delete(relationId, userId);
    }
}

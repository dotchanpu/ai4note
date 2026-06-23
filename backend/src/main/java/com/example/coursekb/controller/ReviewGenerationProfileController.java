package com.example.coursekb.controller;

import com.example.coursekb.dto.ReviewGenerationProfileRequest;
import com.example.coursekb.service.ReviewGenerationProfileService;
import com.example.coursekb.vo.ReviewGenerationProfileVO;
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
@RequestMapping("/api/review-profiles")
public class ReviewGenerationProfileController {
    private final ReviewGenerationProfileService reviewGenerationProfileService;

    public ReviewGenerationProfileController(
            ReviewGenerationProfileService reviewGenerationProfileService) {
        this.reviewGenerationProfileService = reviewGenerationProfileService;
    }

    @GetMapping
    public List<ReviewGenerationProfileVO> list(
            @RequestParam Long userId,
            @RequestParam Long courseId) {
        return reviewGenerationProfileService.list(userId, courseId);
    }

    @PostMapping
    public ReviewGenerationProfileVO create(
            @Valid @RequestBody ReviewGenerationProfileRequest request) {
        return reviewGenerationProfileService.create(request);
    }

    @PutMapping("/{profileId}")
    public ReviewGenerationProfileVO update(
            @PathVariable Long profileId,
            @Valid @RequestBody ReviewGenerationProfileRequest request) {
        return reviewGenerationProfileService.update(profileId, request);
    }

    @DeleteMapping("/{profileId}")
    public void delete(
            @PathVariable Long profileId,
            @RequestParam Long userId) {
        reviewGenerationProfileService.delete(profileId, userId);
    }
}

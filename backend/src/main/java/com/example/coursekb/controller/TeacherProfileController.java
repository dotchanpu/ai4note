package com.example.coursekb.controller;

import com.example.coursekb.dto.TeacherProfileAnalyzeRequest;
import com.example.coursekb.dto.TeacherProfileUpdateRequest;
import com.example.coursekb.service.TeacherProfileService;
import com.example.coursekb.vo.TeacherProfileEvidenceVO;
import com.example.coursekb.vo.TeacherProfileVO;
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
@RequestMapping("/api")
public class TeacherProfileController {
    private final TeacherProfileService teacherProfileService;

    public TeacherProfileController(TeacherProfileService teacherProfileService) {
        this.teacherProfileService = teacherProfileService;
    }

    @GetMapping("/courses/{courseId}/teacher-profiles")
    public List<TeacherProfileVO> list(
            @PathVariable Long courseId,
            @RequestParam Long userId) {
        return teacherProfileService.list(courseId, userId);
    }

    @PostMapping("/courses/{courseId}/teacher-profiles/analyze")
    public TeacherProfileVO analyze(
            @PathVariable Long courseId,
            @Valid @RequestBody TeacherProfileAnalyzeRequest request) {
        return teacherProfileService.analyze(courseId, request);
    }

    @GetMapping("/teacher-profiles/{profileId}/evidence")
    public List<TeacherProfileEvidenceVO> listEvidence(
            @PathVariable Long profileId,
            @RequestParam Long userId) {
        return teacherProfileService.listEvidence(profileId, userId);
    }

    @PutMapping("/teacher-profiles/{profileId}")
    public TeacherProfileVO update(
            @PathVariable Long profileId,
            @Valid @RequestBody TeacherProfileUpdateRequest request) {
        return teacherProfileService.update(profileId, request);
    }
}

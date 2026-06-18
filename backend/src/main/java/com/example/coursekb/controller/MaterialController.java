package com.example.coursekb.controller;

import com.example.coursekb.service.MaterialService;
import com.example.coursekb.vo.MaterialVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/courses/{courseId}/materials")
    public List<MaterialVO> list(
            @PathVariable Long courseId,
            @RequestParam Long userId) {
        return materialService.list(courseId, userId);
    }

    @GetMapping("/materials/{materialId}")
    public MaterialVO detail(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        return materialService.detail(materialId, userId);
    }

    @PostMapping("/materials")
    public MaterialVO upload(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam String title,
            @RequestParam String materialType,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "false") Boolean isKey,
            @RequestParam(required = false) String summary,
            @RequestParam MultipartFile file) {
        return materialService.upload(
                userId,
                courseId,
                chapterId,
                title,
                materialType,
                year,
                isKey,
                summary,
                file);
    }
}

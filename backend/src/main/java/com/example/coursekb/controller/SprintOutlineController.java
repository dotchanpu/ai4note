package com.example.coursekb.controller;

import com.example.coursekb.dto.SprintOutlineGenerateRequest;
import com.example.coursekb.service.SprintOutlineService;
import com.example.coursekb.vo.SprintOutlineGenerateResultVO;
import java.nio.file.Path;
import javax.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SprintOutlineController {
    private final SprintOutlineService sprintOutlineService;

    public SprintOutlineController(SprintOutlineService sprintOutlineService) {
        this.sprintOutlineService = sprintOutlineService;
    }

    @PostMapping("/courses/{courseId}/sprint-outlines/generate")
    public SprintOutlineGenerateResultVO generate(
            @PathVariable Long courseId,
            @Valid @RequestBody SprintOutlineGenerateRequest request) {
        return sprintOutlineService.generate(courseId, request);
    }

    @GetMapping("/sprint-outlines/{taskId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long taskId,
            @RequestParam Long userId) {
        Path path = sprintOutlineService.resolveDownloadPath(taskId, userId);
        String filename = sprintOutlineService.buildDownloadFilename(taskId, userId);
        Resource resource = new FileSystemResource(path.toFile());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(resource);
    }
}

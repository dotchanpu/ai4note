package com.example.coursekb.controller;

import com.example.coursekb.dto.MockExamGenerateRequest;
import com.example.coursekb.service.MockExamService;
import com.example.coursekb.vo.MockExamGenerateResultVO;
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
public class MockExamController {
    private final MockExamService mockExamService;

    public MockExamController(MockExamService mockExamService) {
        this.mockExamService = mockExamService;
    }

    @PostMapping("/courses/{courseId}/mock-exams/generate")
    public MockExamGenerateResultVO generate(
            @PathVariable Long courseId,
            @Valid @RequestBody MockExamGenerateRequest request) {
        return mockExamService.generate(courseId, request);
    }

    @GetMapping("/mock-exams/{taskId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long taskId,
            @RequestParam Long userId) {
        Path path = mockExamService.resolveDownloadPath(taskId, userId);
        String filename = mockExamService.buildDownloadFilename(taskId, userId);
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

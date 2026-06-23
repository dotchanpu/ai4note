package com.example.coursekb.controller;

import com.example.coursekb.dto.AiGenerationTaskStatusRequest;
import com.example.coursekb.service.AiGenerationTaskService;
import com.example.coursekb.vo.AiGenerationTaskVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-generation-tasks")
public class AiGenerationTaskController {
    private final AiGenerationTaskService aiGenerationTaskService;

    public AiGenerationTaskController(AiGenerationTaskService aiGenerationTaskService) {
        this.aiGenerationTaskService = aiGenerationTaskService;
    }

    @GetMapping
    public List<AiGenerationTaskVO> list(
            @RequestParam Long userId,
            @RequestParam(required = false) Long courseId) {
        return aiGenerationTaskService.list(userId, courseId);
    }

    @PutMapping("/{taskId}/status")
    public AiGenerationTaskVO updateStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody AiGenerationTaskStatusRequest request) {
        return aiGenerationTaskService.updateStatus(taskId, request);
    }
}

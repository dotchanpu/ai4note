package com.example.coursekb.controller;

import com.example.coursekb.dto.AiProviderConfigRequest;
import com.example.coursekb.service.AiProviderConfigService;
import com.example.coursekb.vo.AiProviderConfigVO;
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
@RequestMapping("/api/ai/providers")
public class AiProviderConfigController {
    private final AiProviderConfigService aiProviderConfigService;

    public AiProviderConfigController(AiProviderConfigService aiProviderConfigService) {
        this.aiProviderConfigService = aiProviderConfigService;
    }

    @GetMapping
    public List<AiProviderConfigVO> list(@RequestParam Long userId) {
        return aiProviderConfigService.list(userId);
    }

    @PostMapping
    public AiProviderConfigVO create(@Valid @RequestBody AiProviderConfigRequest request) {
        return aiProviderConfigService.create(request);
    }

    @PutMapping("/{configId}")
    public AiProviderConfigVO update(
            @PathVariable Long configId,
            @Valid @RequestBody AiProviderConfigRequest request) {
        return aiProviderConfigService.update(configId, request);
    }

    @DeleteMapping("/{configId}")
    public void delete(@PathVariable Long configId, @RequestParam Long userId) {
        aiProviderConfigService.delete(configId, userId);
    }
}

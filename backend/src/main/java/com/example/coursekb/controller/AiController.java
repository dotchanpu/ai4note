package com.example.coursekb.controller;

import com.example.coursekb.dto.AiChatRequest;
import com.example.coursekb.service.DeepSeekService;
import com.example.coursekb.vo.AiChatResponseVO;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final DeepSeekService deepSeekService;

    public AiController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return deepSeekService.status();
    }

    @PostMapping("/chat")
    public AiChatResponseVO chat(@Valid @RequestBody AiChatRequest request) {
        return deepSeekService.chat(request);
    }
}

package com.example.coursekb.controller;

import com.example.coursekb.dto.AiKnowledgeGenerateRequest;
import com.example.coursekb.dto.KnowledgeItemRequest;
import com.example.coursekb.dto.MaterialTagsRequest;
import com.example.coursekb.service.AiKnowledgeService;
import com.example.coursekb.service.KnowledgeItemService;
import com.example.coursekb.service.TagService;
import com.example.coursekb.vo.AiKnowledgeGenerateResultVO;
import com.example.coursekb.vo.KnowledgeItemVO;
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
@RequestMapping("/api")
public class KnowledgeController {
    private final TagService tagService;
    private final KnowledgeItemService knowledgeItemService;
    private final AiKnowledgeService aiKnowledgeService;

    public KnowledgeController(
            TagService tagService,
            KnowledgeItemService knowledgeItemService,
            AiKnowledgeService aiKnowledgeService) {
        this.tagService = tagService;
        this.knowledgeItemService = knowledgeItemService;
        this.aiKnowledgeService = aiKnowledgeService;
    }

    @GetMapping("/courses/{courseId}/tags")
    public List<String> listCourseTags(
            @PathVariable Long courseId, @RequestParam Long userId) {
        return tagService.listCourseTags(courseId, userId);
    }

    @GetMapping("/materials/{materialId}/tags")
    public List<String> listMaterialTags(
            @PathVariable Long materialId, @RequestParam Long userId) {
        return tagService.listMaterialTags(materialId, userId);
    }

    @PutMapping("/materials/{materialId}/tags")
    public List<String> replaceMaterialTags(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @RequestBody MaterialTagsRequest request) {
        return tagService.replaceMaterialTags(materialId, userId, request);
    }

    @GetMapping("/courses/{courseId}/knowledge-items")
    public List<KnowledgeItemVO> listKnowledgeItems(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String itemType) {
        return knowledgeItemService.list(
                courseId, userId, materialId, chapterId, itemType);
    }

    @PostMapping("/courses/{courseId}/knowledge-items")
    public KnowledgeItemVO createKnowledgeItem(
            @PathVariable Long courseId,
            @RequestParam Long userId,
            @Valid @RequestBody KnowledgeItemRequest request) {
        return knowledgeItemService.create(courseId, userId, request);
    }

    @PutMapping("/courses/{courseId}/knowledge-items/{itemId}")
    public KnowledgeItemVO updateKnowledgeItem(
            @PathVariable Long courseId,
            @PathVariable Long itemId,
            @RequestParam Long userId,
            @Valid @RequestBody KnowledgeItemRequest request) {
        return knowledgeItemService.update(courseId, itemId, userId, request);
    }

    @DeleteMapping("/courses/{courseId}/knowledge-items/{itemId}")
    public void deleteKnowledgeItem(
            @PathVariable Long courseId,
            @PathVariable Long itemId,
            @RequestParam Long userId) {
        knowledgeItemService.delete(courseId, itemId, userId);
    }

    @PostMapping("/materials/{materialId}/knowledge-items/ai-generate")
    public AiKnowledgeGenerateResultVO generateKnowledgeItems(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @Valid @RequestBody AiKnowledgeGenerateRequest request) {
        return aiKnowledgeService.generate(materialId, userId, request);
    }
}

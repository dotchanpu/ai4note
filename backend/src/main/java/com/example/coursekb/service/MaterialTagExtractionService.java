package com.example.coursekb.service;

import com.example.coursekb.dto.MaterialTagPreviewRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.MaterialTagPreviewVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MaterialTagExtractionService {
    private static final int MAX_SOURCE_LENGTH = 45000;

    private final MaterialService materialService;
    private final TextChunkRepository textChunkRepository;
    private final DeepSeekService deepSeekService;
    private final AiGenerationTaskService aiGenerationTaskService;
    private final ObjectMapper objectMapper;

    public MaterialTagExtractionService(
            MaterialService materialService,
            TextChunkRepository textChunkRepository,
            DeepSeekService deepSeekService,
            AiGenerationTaskService aiGenerationTaskService,
            ObjectMapper objectMapper) {
        this.materialService = materialService;
        this.textChunkRepository = textChunkRepository;
        this.deepSeekService = deepSeekService;
        this.aiGenerationTaskService = aiGenerationTaskService;
        this.objectMapper = objectMapper;
    }

    public MaterialTagPreviewVO preview(Long materialId, Long userId, MaterialTagPreviewRequest request) {
        Material material = materialService.getOwnedMaterial(materialId, userId);
        List<TextChunk> chunks = textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(materialId);
        if (chunks.isEmpty()) {
            throw new BusinessException("请先解析资料文本，再提取关键词标签");
        }

        String source = buildSource(chunks);
        String systemPrompt = buildSystemPrompt();
        String userPrompt = "资料标题：" + material.getTitle()
                + "\n已有摘要：" + valueOrDefault(material.getSummary(), "无")
                + "\n资料类型：" + material.getMaterialType()
                + "\n\n解析正文：\n" + source;
        String prompt = systemPrompt + "\n\n" + userPrompt;
        AiGenerationTask task = aiGenerationTaskService.createTask(
                userId,
                material.getCourseId(),
                "MATERIAL_TAG_EXTRACTION",
                prompt,
                null,
                null,
                null);
        aiGenerationTaskService.markRunning(task.getId());
        try {
            String json = deepSeekService.generateJson(
                    userId,
                    material.getCourseId(),
                    systemPrompt,
                    userPrompt,
                    request == null ? null : request.getModel(),
                    2048);
            List<String> tags = parseTags(json);
            if (tags.isEmpty()) {
                throw new BusinessException("AI 未提取出有效关键词标签");
            }
            aiGenerationTaskService.markSuccess(task.getId(), "material-tags:" + tags.size());
            return new MaterialTagPreviewVO(materialId, tags);
        } catch (RuntimeException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    private String buildSystemPrompt() {
        return "你是 AI4Note 的课程资料关键词提取助手。请只基于用户提供的解析正文提取资料标签，"
                + "不要补充原文没有的信息。只输出一个合法 JSON 对象，不要输出 Markdown。"
                + "格式必须为：{\"tags\":[\"标签1\",\"标签2\"]}。"
                + "要求：输出 5 到 12 个中文短标签；每个标签 2 到 20 个字；"
                + "优先选择课程概念、方法、公式、题型、实验主题或复习场景；合并近义重复标签。";
    }

    private String buildSource(List<TextChunk> chunks) {
        StringBuilder source = new StringBuilder();
        for (TextChunk chunk : chunks) {
            String block = "\n[第 " + (chunk.getPageNo() == null ? "?" : chunk.getPageNo())
                    + " 页]\n" + chunk.getContent() + "\n";
            if (source.length() + block.length() > MAX_SOURCE_LENGTH) {
                break;
            }
            source.append(block);
        }
        return source.toString();
    }

    private List<String> parseTags(String value) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(value));
            JsonNode node = root.path("tags");
            if (!node.isArray()) {
                node = root.path("keywords");
            }
            Set<String> tags = new LinkedHashSet<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    String tag = item.asText("").trim().replaceAll("\\s+", " ");
                    if (!tag.isEmpty() && tag.length() <= 64) {
                        tags.add(tag);
                    }
                    if (tags.size() >= 12) {
                        break;
                    }
                }
            }
            return new ArrayList<>(tags);
        } catch (Exception exception) {
            throw new BusinessException("AI 返回关键词格式不正确，请重试");
        }
    }

    private String stripCodeFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}

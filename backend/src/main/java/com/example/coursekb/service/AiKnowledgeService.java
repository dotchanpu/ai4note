package com.example.coursekb.service;

import com.example.coursekb.dto.AiKnowledgeGenerateRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.AiKnowledgeGenerateResultVO;
import com.example.coursekb.vo.KnowledgeItemVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiKnowledgeService {
    private static final int MAX_SOURCE_LENGTH = 45000;

    private final MaterialService materialService;
    private final TextChunkRepository textChunkRepository;
    private final DeepSeekService deepSeekService;
    private final AiGenerationTaskService aiGenerationTaskService;
    private final TagService tagService;
    private final KnowledgeItemService knowledgeItemService;
    private final ObjectMapper objectMapper;

    public AiKnowledgeService(
            MaterialService materialService,
            TextChunkRepository textChunkRepository,
            DeepSeekService deepSeekService,
            AiGenerationTaskService aiGenerationTaskService,
            TagService tagService,
            KnowledgeItemService knowledgeItemService,
            ObjectMapper objectMapper) {
        this.materialService = materialService;
        this.textChunkRepository = textChunkRepository;
        this.deepSeekService = deepSeekService;
        this.aiGenerationTaskService = aiGenerationTaskService;
        this.tagService = tagService;
        this.knowledgeItemService = knowledgeItemService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiKnowledgeGenerateResultVO generate(
            Long materialId,
            Long userId,
            AiKnowledgeGenerateRequest request) {
        Material material = materialService.getOwnedMaterial(materialId, userId);
        List<TextChunk> chunks = textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(materialId);
        if (chunks.isEmpty()) {
            throw new BusinessException("请先解析 PDF，再使用 AI 整理知识条目");
        }
        int maxItems = request.getMaxItems() == null ? 12 : request.getMaxItems();
        String source = buildSource(chunks);
        String systemPrompt = buildSystemPrompt(maxItems);
        String userPrompt = "资料标题：" + material.getTitle() + "\n\n资料正文：\n" + source;
        String prompt = systemPrompt + "\n\n" + userPrompt;
        AiGenerationTask task = aiGenerationTaskService.createTask(
                userId,
                material.getCourseId(),
                "KNOWLEDGE_EXTRACTION",
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
                    request.getModel(),
                    8192);

            JsonNode root = parseJson(json);
            List<String> tags = parseTags(root.path("tags"));
            List<KnowledgeItem> items = parseItems(root.path("items"), material, maxItems);
            if (items.isEmpty()) {
                throw new BusinessException("AI 未整理出有效知识条目，请调整资料后重试");
            }

            List<String> savedTags = tagService.replaceMaterialTagsInternal(material, tags);
            List<KnowledgeItemVO> savedItems = knowledgeItemService.replaceMaterialItems(
                    material,
                    items,
                    Boolean.TRUE.equals(request.getReplaceExisting()));
            aiGenerationTaskService.markSuccess(task.getId(), "knowledge-items:" + savedItems.size());
            return new AiKnowledgeGenerateResultVO(savedTags, savedItems);
        } catch (RuntimeException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    private String buildSystemPrompt(int maxItems) {
        return "你是课程知识工程师。请基于资料原文理解语义并整理知识，而不是做关键词提取。"
                + "只输出一个合法 JSON 对象，不要输出 Markdown。格式必须为："
                + "{\"tags\":[\"标签\"],\"items\":[{\"title\":\"标题\","
                + "\"itemType\":\"DEFINITION|KEY_POINT|FORMULA|METHOD|EXAMPLE|WARNING\","
                + "\"content\":\"完整且可独立理解的知识说明\","
                + "\"sourcePage\":1,\"importanceLevel\":1}]}。"
                + "要求：最多 " + maxItems + " 个条目；标签 3-10 个；"
                + "条目需合并重复概念，保留定义、原理、步骤、公式、典型例子和易错点；"
                + "sourcePage 必须来自原文页码；importanceLevel 为 1-5；"
                + "不能补充资料中没有依据的事实。";
    }

    private String buildSource(List<TextChunk> chunks) {
        StringBuilder source = new StringBuilder();
        for (TextChunk chunk : chunks) {
            String block = "\n[第" + (chunk.getPageNo() == null ? "?" : chunk.getPageNo())
                    + "页]\n" + chunk.getContent() + "\n";
            if (source.length() + block.length() > MAX_SOURCE_LENGTH) {
                break;
            }
            source.append(block);
        }
        return source.toString();
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(stripCodeFence(value));
        } catch (Exception exception) {
            throw new BusinessException("AI 返回格式不正确，请重试");
        }
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private List<String> parseTags(JsonNode node) {
        Set<String> tags = new LinkedHashSet<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = item.asText("").trim();
                if (!value.isEmpty() && value.length() <= 64) {
                    tags.add(value);
                }
                if (tags.size() >= 10) {
                    break;
                }
            }
        }
        return new ArrayList<>(tags);
    }

    private List<KnowledgeItem> parseItems(JsonNode node, Material material, int maxItems) {
        List<KnowledgeItem> items = new ArrayList<>();
        if (!node.isArray()) {
            return items;
        }
        for (JsonNode value : node) {
            String title = value.path("title").asText("").trim();
            String content = value.path("content").asText("").trim();
            String itemType = normalizeAiType(value.path("itemType").asText(""));
            if (title.isEmpty() || content.isEmpty() || itemType == null) {
                continue;
            }
            KnowledgeItem item = new KnowledgeItem();
            item.setCourseId(material.getCourseId());
            item.setMaterialId(material.getId());
            item.setChapterId(material.getChapterId());
            item.setTitle(title.length() > 255 ? title.substring(0, 255) : title);
            item.setItemType(itemType);
            item.setContent(content);
            item.setSourcePage(value.path("sourcePage").isInt()
                    ? value.path("sourcePage").asInt()
                    : null);
            int importance = value.path("importanceLevel").asInt(3);
            item.setImportanceLevel(Math.max(1, Math.min(5, importance)));
            items.add(item);
            if (items.size() >= maxItems) {
                break;
            }
        }
        return items;
    }

    private String normalizeAiType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "DEFINITION":
            case "KEY_POINT":
            case "FORMULA":
            case "METHOD":
            case "EXAMPLE":
            case "WARNING":
                return normalized;
            default:
                return null;
        }
    }
}

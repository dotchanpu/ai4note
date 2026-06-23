package com.example.coursekb.service;

import com.example.coursekb.dto.MaterialSummaryGenerateRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.MaterialVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialSummaryService {
    private static final int MAX_SOURCE_LENGTH = 45000;
    private static final int MAX_SUMMARY_LENGTH = 2000;

    private final MaterialService materialService;
    private final MaterialRepository materialRepository;
    private final TextChunkRepository textChunkRepository;
    private final DeepSeekService deepSeekService;
    private final AiGenerationTaskService aiGenerationTaskService;
    private final ObjectMapper objectMapper;

    public MaterialSummaryService(
            MaterialService materialService,
            MaterialRepository materialRepository,
            TextChunkRepository textChunkRepository,
            DeepSeekService deepSeekService,
            AiGenerationTaskService aiGenerationTaskService,
            ObjectMapper objectMapper) {
        this.materialService = materialService;
        this.materialRepository = materialRepository;
        this.textChunkRepository = textChunkRepository;
        this.deepSeekService = deepSeekService;
        this.aiGenerationTaskService = aiGenerationTaskService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MaterialVO generate(Long materialId, Long userId, MaterialSummaryGenerateRequest request) {
        Material material = materialService.getOwnedMaterial(materialId, userId);
        List<TextChunk> chunks = textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(materialId);
        if (chunks.isEmpty()) {
            throw new BusinessException("请先解析资料文本，再生成摘要");
        }

        String source = buildSource(chunks);
        String systemPrompt = buildSystemPrompt();
        String userPrompt = "资料标题：" + material.getTitle()
                + "\n资料类型：" + material.getMaterialType()
                + "\n\n解析正文：\n" + source;
        String prompt = systemPrompt + "\n\n" + userPrompt;
        AiGenerationTask task = aiGenerationTaskService.createTask(
                userId,
                material.getCourseId(),
                "MATERIAL_SUMMARY",
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
            String summary = parseSummary(json);
            material.setSummary(summary);
            materialRepository.save(material);
            aiGenerationTaskService.markSuccess(task.getId(), "material-summary:" + materialId);
            return materialService.detail(materialId, userId);
        } catch (RuntimeException exception) {
            aiGenerationTaskService.markFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }

    private String buildSystemPrompt() {
        return "你是 AI4Note 的课程资料摘要助手。请只基于用户提供的解析正文生成资料摘要，"
                + "不要补充原文没有的信息。只输出一个合法 JSON 对象，不要输出 Markdown。"
                + "格式必须为：{\"summary\":\"120-260 字中文摘要\"}。"
                + "摘要应覆盖资料主题、核心内容、适合复习时关注的重点和使用场景，语言简洁准确。";
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

    private String parseSummary(String value) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(value));
            String summary = root.path("summary").asText("").trim();
            if (summary.isEmpty()) {
                throw new BusinessException("AI 未返回有效摘要");
            }
            return summary.length() <= MAX_SUMMARY_LENGTH
                    ? summary
                    : summary.substring(0, MAX_SUMMARY_LENGTH);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("AI 返回摘要格式不正确，请重试");
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
}

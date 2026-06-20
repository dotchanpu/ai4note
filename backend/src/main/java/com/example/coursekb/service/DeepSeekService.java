package com.example.coursekb.service;

import com.example.coursekb.dto.AiChatRequest;
import com.example.coursekb.entity.Course;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.vo.AiChatResponseVO;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class DeepSeekService {
    private static final List<String> SUPPORTED_MODELS =
            Arrays.asList("deepseek-v4-flash", "deepseek-v4-pro");

    private final CourseService courseService;
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;

    public DeepSeekService(
            CourseService courseService,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${ai4note.ai.deepseek.base-url}") String baseUrl,
            @Value("${ai4note.ai.deepseek.api-key:}") String apiKey,
            @Value("${ai4note.ai.deepseek.default-model}") String defaultModel) {
        this.courseService = courseService;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.defaultModel = defaultModel;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "DeepSeek");
        result.put("configured", isConfigured());
        result.put("baseUrl", baseUrl);
        result.put("defaultModel", defaultModel);
        result.put("supportedModels", SUPPORTED_MODELS);
        return result;
    }

    public AiChatResponseVO chat(AiChatRequest request) {
        ensureConfigured();
        Course course = courseService.getOwnedCourse(request.getCourseId(), request.getUserId());
        String model = normalizeModel(request.getModel());
        int maxTokens = normalizeMaxTokens(request.getMaxTokens());
        boolean thinking = Boolean.TRUE.equals(request.getThinking());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", buildMessages(course, request.getMessage().trim()));
        body.put("stream", false);
        body.put("max_tokens", maxTokens);
        body.put("user_id", "ai4note-" + request.getUserId());

        Map<String, String> thinkingConfig = new LinkedHashMap<>();
        thinkingConfig.put("type", thinking ? "enabled" : "disabled");
        body.put("thinking", thinkingConfig);
        if (thinking) {
            body.put("reasoning_effort", "high");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class);
            return parseResponse(response.getBody());
        } catch (HttpStatusCodeException exception) {
            throw mapProviderError(exception);
        } catch (RestClientException exception) {
            throw new BusinessException("DeepSeek 服务连接失败，请稍后重试");
        }
    }

    public String generateJson(
            Long userId,
            Long courseId,
            String systemPrompt,
            String userPrompt,
            String requestedModel,
            int maxTokens) {
        ensureConfigured();
        courseService.getOwnedCourse(courseId, userId);
        String model = normalizeModel(requestedModel);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);
        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("max_tokens", normalizeMaxTokens(maxTokens));
        body.put("user_id", "ai4note-" + userId);
        Map<String, String> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);
        Map<String, String> thinkingConfig = new LinkedHashMap<>();
        thinkingConfig.put("type", "disabled");
        body.put("thinking", thinkingConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class);
            JsonNode root = response.getBody();
            if (root == null || !root.path("choices").isArray() || root.path("choices").isEmpty()) {
                throw new BusinessException("DeepSeek 返回了空响应");
            }
            String content = textOrNull(root.path("choices").get(0).path("message").path("content"));
            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException("DeepSeek 未返回结构化内容");
            }
            return content;
        } catch (HttpStatusCodeException exception) {
            throw mapProviderError(exception);
        } catch (RestClientException exception) {
            throw new BusinessException("DeepSeek 服务连接失败，请稍后重试");
        }
    }

    private List<Map<String, String>> buildMessages(Course course, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put(
                "content",
                "你是 AI4Note 的课程学习助手。当前课程是《"
                        + course.getCourseName()
                        + "》"
                        + optionalCourseContext(course)
                        + "。请使用中文给出准确、结构清晰的回答；"
                        + "不确定时明确说明，不要虚构课程资料内容。");
        messages.add(system);

        Map<String, String> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userMessage);
        messages.add(user);
        return messages;
    }

    private String optionalCourseContext(Course course) {
        StringBuilder context = new StringBuilder();
        if (course.getCourseCode() != null && !course.getCourseCode().trim().isEmpty()) {
            context.append("，课程编号为 ").append(course.getCourseCode());
        }
        if (course.getSemester() != null && !course.getSemester().trim().isEmpty()) {
            context.append("，学期为 ").append(course.getSemester());
        }
        if (course.getDescription() != null && !course.getDescription().trim().isEmpty()) {
            context.append("，课程简介：").append(course.getDescription().trim());
        }
        return context.toString();
    }

    private AiChatResponseVO parseResponse(JsonNode root) {
        if (root == null || !root.path("choices").isArray() || root.path("choices").isEmpty()) {
            throw new BusinessException("DeepSeek 返回了空响应");
        }
        JsonNode choice = root.path("choices").get(0);
        JsonNode message = choice.path("message");
        AiChatResponseVO result = new AiChatResponseVO();
        result.setRequestId(textOrNull(root.path("id")));
        result.setModel(textOrNull(root.path("model")));
        result.setContent(textOrNull(message.path("content")));
        result.setReasoningContent(textOrNull(message.path("reasoning_content")));
        result.setFinishReason(textOrNull(choice.path("finish_reason")));

        JsonNode usage = root.path("usage");
        result.setPromptTokens(integerOrNull(usage.path("prompt_tokens")));
        result.setCompletionTokens(integerOrNull(usage.path("completion_tokens")));
        result.setTotalTokens(integerOrNull(usage.path("total_tokens")));
        return result;
    }

    private BusinessException mapProviderError(HttpStatusCodeException exception) {
        int status = exception.getRawStatusCode();
        if (status == 401) {
            return new BusinessException("DeepSeek API Key 无效，请检查配置");
        }
        if (status == 402) {
            return new BusinessException("DeepSeek 账户余额不足");
        }
        if (status == 429) {
            return new BusinessException("DeepSeek 请求过于频繁，请稍后重试");
        }
        if (status == 503) {
            return new BusinessException("DeepSeek 服务繁忙，请稍后重试");
        }
        return new BusinessException("DeepSeek 请求失败，状态码：" + status);
    }

    private String normalizeModel(String requestedModel) {
        String model = requestedModel == null || requestedModel.trim().isEmpty()
                ? defaultModel
                : requestedModel.trim();
        if (!SUPPORTED_MODELS.contains(model)) {
            throw new BusinessException("不支持的 DeepSeek 模型：" + model);
        }
        return model;
    }

    private int normalizeMaxTokens(Integer requestedMaxTokens) {
        int value = requestedMaxTokens == null ? 2048 : requestedMaxTokens;
        if (value < 1 || value > 8192) {
            throw new BusinessException("maxTokens 必须在 1 到 8192 之间");
        }
        return value;
    }

    private boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new BusinessException("尚未配置 DeepSeek API Key");
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private Integer integerOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }
}

package com.example.coursekb.service;

import com.example.coursekb.dto.AiProviderConfigRequest;
import com.example.coursekb.entity.AiProviderConfig;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.AiProviderConfigRepository;
import com.example.coursekb.vo.AiProviderConfigVO;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiProviderConfigService {
    private static final Pattern ENV_ALIAS_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{1,127}");

    private final AiProviderConfigRepository aiProviderConfigRepository;

    public AiProviderConfigService(AiProviderConfigRepository aiProviderConfigRepository) {
        this.aiProviderConfigRepository = aiProviderConfigRepository;
    }

    public List<AiProviderConfigVO> list(Long userId) {
        return aiProviderConfigRepository.findByUserIdOrderByEnabledDescUpdateTimeDescIdDesc(userId)
                .stream()
                .map(AiProviderConfigVO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AiProviderConfigVO create(AiProviderConfigRequest request) {
        AiProviderConfig config = new AiProviderConfig();
        apply(config, request);
        return AiProviderConfigVO.from(aiProviderConfigRepository.save(config));
    }

    @Transactional
    public AiProviderConfigVO update(Long configId, AiProviderConfigRequest request) {
        AiProviderConfig config = getOwnedConfig(configId, request.getUserId());
        apply(config, request);
        return AiProviderConfigVO.from(aiProviderConfigRepository.save(config));
    }

    @Transactional
    public void delete(Long configId, Long userId) {
        AiProviderConfig config = getOwnedConfig(configId, userId);
        aiProviderConfigRepository.delete(config);
    }

    private AiProviderConfig getOwnedConfig(Long configId, Long userId) {
        return aiProviderConfigRepository.findByIdAndUserId(configId, userId)
                .orElseThrow(() -> new BusinessException("AI 服务配置不存在或无权访问"));
    }

    private void apply(AiProviderConfig config, AiProviderConfigRequest request) {
        config.setUserId(request.getUserId());
        config.setProviderName(requireText(request.getProviderName(), "供应商名称不能为空"));
        config.setBaseUrl(normalizeBaseUrl(request.getBaseUrl()));
        config.setModelName(requireText(request.getModelName(), "模型名称不能为空"));
        config.setApiKeyAlias(normalizeApiKeyAlias(request.getApiKeyAlias()));
        config.setEnabled(!Boolean.FALSE.equals(request.getEnabled()));
    }

    private String normalizeBaseUrl(String value) {
        String normalized = requireText(value, "Base URL 不能为空");
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            throw new BusinessException("Base URL 必须以 http:// 或 https:// 开头");
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeApiKeyAlias(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        if (!ENV_ALIAS_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException("API Key 别名必须是环境变量名，例如 DEEPSEEK_API_KEY");
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}

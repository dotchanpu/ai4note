package com.example.coursekb.service;

import com.example.coursekb.entity.Material;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.vo.MaterialSimilarityVO;
import com.example.coursekb.vo.MaterialVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MaterialSimilarityService {
    private static final double MIN_SCORE = 0.42;
    private static final int MAX_TEXT_LENGTH = 20000;

    private final MaterialService materialService;
    private final MaterialRepository materialRepository;
    private final JdbcTemplate jdbcTemplate;

    public MaterialSimilarityService(
            MaterialService materialService,
            MaterialRepository materialRepository,
            JdbcTemplate jdbcTemplate) {
        this.materialService = materialService;
        this.materialRepository = materialRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MaterialSimilarityVO> detect(Long materialId, Long userId) {
        Material source = materialService.getOwnedMaterial(materialId, userId);
        List<String> sourceTags = loadTags(materialId);
        String sourceText = loadParsedText(materialId);
        Set<String> sourceTokens = tokensFor(source, sourceTags, sourceText);

        return materialRepository.findByCourseIdOrderByUploadTimeDesc(source.getCourseId()).stream()
                .filter(candidate -> !candidate.getId().equals(materialId))
                .map(candidate -> compare(source, sourceTags, sourceText, sourceTokens, candidate, userId))
                .filter(result -> result.getScore() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(MaterialSimilarityVO::getScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private MaterialSimilarityVO compare(
            Material source,
            List<String> sourceTags,
            String sourceText,
            Set<String> sourceTokens,
            Material candidate,
            Long userId) {
        List<String> candidateTags = loadTags(candidate.getId());
        String candidateText = loadParsedText(candidate.getId());
        Set<String> candidateTokens = tokensFor(candidate, candidateTags, candidateText);

        double titleScore = jaccard(tokenize(source.getTitle()), tokenize(candidate.getTitle()));
        double summaryScore = jaccard(tokenize(source.getSummary()), tokenize(candidate.getSummary()));
        double tagScore = jaccard(
                sourceTags.stream().map(this::normalize).collect(Collectors.toSet()),
                candidateTags.stream().map(this::normalize).collect(Collectors.toSet()));
        double textScore = jaccard(tokenize(sourceText), tokenize(candidateText));
        double combinedScore = Math.max(
                jaccard(sourceTokens, candidateTokens),
                Math.max(Math.max(titleScore * 0.9, summaryScore * 0.75), Math.max(tagScore * 0.8, textScore)));

        List<String> reasons = buildReasons(titleScore, summaryScore, tagScore, textScore);
        return new MaterialSimilarityVO(
                materialService.detail(candidate.getId(), userId),
                Math.round(combinedScore * 100.0) / 100.0,
                reasons);
    }

    private List<String> buildReasons(double titleScore, double summaryScore, double tagScore, double textScore) {
        List<String> reasons = new ArrayList<>();
        if (titleScore >= 0.45) {
            reasons.add("标题相似");
        }
        if (summaryScore >= 0.35) {
            reasons.add("摘要相似");
        }
        if (tagScore >= 0.35) {
            reasons.add("标签重合");
        }
        if (textScore >= 0.32) {
            reasons.add("解析正文相似");
        }
        if (reasons.isEmpty()) {
            reasons.add("综合内容相似");
        }
        return reasons;
    }

    private Set<String> tokensFor(Material material, List<String> tags, String text) {
        Set<String> tokens = new HashSet<>();
        tokens.addAll(tokenize(material.getTitle()));
        tokens.addAll(tokenize(material.getSummary()));
        tags.forEach(tag -> tokens.addAll(tokenize(tag)));
        tokens.addAll(tokenize(text));
        return tokens;
    }

    private List<String> loadTags(Long materialId) {
        return jdbcTemplate.queryForList(
                "SELECT t.tag_name FROM tag t JOIN material_tag mt ON mt.tag_id = t.id "
                        + "WHERE mt.material_id = ? ORDER BY t.tag_name",
                String.class,
                materialId);
    }

    private String loadParsedText(Long materialId) {
        List<String> chunks = jdbcTemplate.queryForList(
                "SELECT content FROM text_chunk WHERE material_id = ? ORDER BY chunk_index",
                String.class,
                materialId);
        StringBuilder builder = new StringBuilder();
        for (String chunk : chunks) {
            if (chunk == null || chunk.trim().isEmpty()) {
                continue;
            }
            if (builder.length() + chunk.length() > MAX_TEXT_LENGTH) {
                int remaining = MAX_TEXT_LENGTH - builder.length();
                if (remaining > 0) {
                    builder.append(' ').append(chunk, 0, Math.min(chunk.length(), remaining));
                }
                break;
            }
            builder.append(' ').append(chunk);
        }
        return builder.toString();
    }

    private Set<String> tokenize(String value) {
        Map<String, Boolean> tokens = new LinkedHashMap<>();
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return tokens.keySet();
        }
        for (String part : normalized.split("[^\\p{L}\\p{N}]+")) {
            if (part.length() >= 2) {
                tokens.put(part, true);
            }
        }
        String compact = normalized.replaceAll("[^\\p{L}\\p{N}]", "");
        if (containsCjk(compact)) {
            for (int index = 0; index < compact.length() - 1; index++) {
                tokens.put(compact.substring(index, index + 2), true);
            }
        }
        return tokens.keySet();
    }

    private double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean containsCjk(String value) {
        for (int index = 0; index < value.length(); index++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(value.charAt(index));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}

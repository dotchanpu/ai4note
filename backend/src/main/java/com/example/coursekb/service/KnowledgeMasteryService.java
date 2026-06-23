package com.example.coursekb.service;

import com.example.coursekb.dto.KnowledgeMasteryRequest;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.UserKnowledgeStatus;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.UserKnowledgeStatusRepository;
import com.example.coursekb.vo.KnowledgeMasteryVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeMasteryService {
    private static final Set<String> MASTERY_STATUSES = new HashSet<>(Arrays.asList(
            "UNKNOWN", "LEARNING", "MASTERED"));
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");

    private final CourseService courseService;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final UserKnowledgeStatusRepository userKnowledgeStatusRepository;

    public KnowledgeMasteryService(
            CourseService courseService,
            KnowledgeItemRepository knowledgeItemRepository,
            UserKnowledgeStatusRepository userKnowledgeStatusRepository) {
        this.courseService = courseService;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.userKnowledgeStatusRepository = userKnowledgeStatusRepository;
    }

    public KnowledgeMasteryVO get(Long knowledgeItemId, Long userId) {
        KnowledgeItem item = getOwnedKnowledgeItem(knowledgeItemId, userId);
        return userKnowledgeStatusRepository.findByUserIdAndKnowledgeItemId(userId, item.getId())
                .map(KnowledgeMasteryVO::from)
                .orElseGet(() -> KnowledgeMasteryVO.defaultFor(userId, item.getId()));
    }

    @Transactional
    public KnowledgeMasteryVO update(Long knowledgeItemId, KnowledgeMasteryRequest request) {
        KnowledgeItem item = getOwnedKnowledgeItem(knowledgeItemId, request.getUserId());
        UserKnowledgeStatus status = userKnowledgeStatusRepository
                .findByUserIdAndKnowledgeItemId(request.getUserId(), item.getId())
                .orElseGet(UserKnowledgeStatus::new);
        status.setUserId(request.getUserId());
        status.setKnowledgeItemId(item.getId());
        status.setMasteryStatus(normalizeMasteryStatus(request.getMasteryStatus()));
        status.setMasteryScore(normalizeScore(request.getMasteryScore()));
        status.setNote(normalizeNote(request.getNote()));
        status.setLastReviewTime(request.getLastReviewTime() == null
                ? LocalDateTime.now()
                : request.getLastReviewTime());
        return KnowledgeMasteryVO.from(userKnowledgeStatusRepository.save(status));
    }

    private KnowledgeItem getOwnedKnowledgeItem(Long knowledgeItemId, Long userId) {
        KnowledgeItem item = knowledgeItemRepository.findById(knowledgeItemId)
                .orElseThrow(() -> new BusinessException("知识条目不存在"));
        courseService.getOwnedCourse(item.getCourseId(), userId);
        return item;
    }

    private String normalizeMasteryStatus(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "UNKNOWN"
                : value.trim().toUpperCase(Locale.ROOT);
        if ("WEAK".equals(normalized) || "NEED_REVIEW".equals(normalized)) {
            return "LEARNING";
        }
        if (!MASTERY_STATUSES.contains(normalized)) {
            throw new BusinessException("不支持的掌握状态：" + normalized);
        }
        return normalized;
    }

    private BigDecimal normalizeScore(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(MIN_SCORE) < 0 || value.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException("掌握分数必须在 0 到 100 之间");
        }
        return value;
    }

    private String normalizeNote(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

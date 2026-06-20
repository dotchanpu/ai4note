package com.example.coursekb.service;

import com.example.coursekb.dto.KnowledgeGapReportRequest;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.CourseRelation;
import com.example.coursekb.entity.KnowledgeGapItem;
import com.example.coursekb.entity.KnowledgeGapReport;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.UserKnowledgeStatus;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.CourseRelationRepository;
import com.example.coursekb.mapper.CourseRepository;
import com.example.coursekb.mapper.KnowledgeGapItemRepository;
import com.example.coursekb.mapper.KnowledgeGapReportRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.UserKnowledgeStatusRepository;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.KnowledgeGapItemVO;
import com.example.coursekb.vo.KnowledgeGapReportVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeGapService {
    private static final DateTimeFormatter REPORT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseService courseService;
    private final ExamQuestionService examQuestionService;
    private final CourseRepository courseRepository;
    private final CourseRelationRepository courseRelationRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final UserKnowledgeStatusRepository userKnowledgeStatusRepository;
    private final KnowledgeGapReportRepository knowledgeGapReportRepository;
    private final KnowledgeGapItemRepository knowledgeGapItemRepository;

    public KnowledgeGapService(
            CourseService courseService,
            ExamQuestionService examQuestionService,
            CourseRepository courseRepository,
            CourseRelationRepository courseRelationRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            UserKnowledgeStatusRepository userKnowledgeStatusRepository,
            KnowledgeGapReportRepository knowledgeGapReportRepository,
            KnowledgeGapItemRepository knowledgeGapItemRepository) {
        this.courseService = courseService;
        this.examQuestionService = examQuestionService;
        this.courseRepository = courseRepository;
        this.courseRelationRepository = courseRelationRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.userKnowledgeStatusRepository = userKnowledgeStatusRepository;
        this.knowledgeGapReportRepository = knowledgeGapReportRepository;
        this.knowledgeGapItemRepository = knowledgeGapItemRepository;
    }

    public List<KnowledgeGapReportVO> listReports(Long courseId, Long userId) {
        Course course = courseService.getOwnedCourse(courseId, userId);
        return knowledgeGapReportRepository
                .findByUserIdAndCourseIdOrderByCreateTimeDescIdDesc(userId, courseId)
                .stream()
                .map(report -> KnowledgeGapReportVO.from(
                        report,
                        course.getCourseName(),
                        knowledgeGapItemRepository.countByReportId(report.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public KnowledgeGapReportVO generate(Long courseId, KnowledgeGapReportRequest request) {
        Course course = courseService.getOwnedCourse(courseId, request.getUserId());
        boolean includePrerequisites = !Boolean.FALSE.equals(request.getIncludePrerequisites());
        List<SourceCourse> sourceCourses = buildSourceCourses(course, request.getUserId(), includePrerequisites);
        Map<Long, SourceCourse> sourceByKnowledgeItemId = new HashMap<>();
        List<KnowledgeItem> allItems = sourceCourses.stream()
                .flatMap(source -> knowledgeItemRepository
                        .findByCourseIdOrderByImportanceLevelDescIdDesc(source.course.getId())
                        .stream()
                        .map(item -> {
                            sourceByKnowledgeItemId.put(item.getId(), source);
                            return item;
                        }))
                .collect(Collectors.toList());

        Map<Long, UserKnowledgeStatus> masteryByItemId = loadMastery(request.getUserId(), allItems);
        Map<Long, ExamKnowledgeStatVO> statByItemId = loadExamStats(sourceCourses, request.getUserId());

        List<KnowledgeGapItem> gapItems = allItems.stream()
                .map(item -> buildGapItem(item, sourceByKnowledgeItemId.get(item.getId()),
                        masteryByItemId.get(item.getId()), statByItemId.get(item.getId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(KnowledgeGapItem::getSeverityLevel).reversed()
                        .thenComparing(KnowledgeGapItem::getKnowledgeItemId))
                .collect(Collectors.toList());

        KnowledgeGapReport report = new KnowledgeGapReport();
        report.setUserId(request.getUserId());
        report.setCourseId(courseId);
        report.setIncludePrerequisites(includePrerequisites);
        report.setReportName(normalizeReportName(request.getReportName(), course.getCourseName()));
        report.setSummary(renderSummary(gapItems, includePrerequisites));
        KnowledgeGapReport savedReport = knowledgeGapReportRepository.save(report);
        for (KnowledgeGapItem item : gapItems) {
            item.setReportId(savedReport.getId());
        }
        knowledgeGapItemRepository.saveAll(gapItems);
        return KnowledgeGapReportVO.from(savedReport, course.getCourseName(), gapItems.size());
    }

    public KnowledgeGapReportVO getReport(Long reportId, Long userId) {
        KnowledgeGapReport report = getOwnedReport(reportId, userId);
        Course course = courseService.getOwnedCourse(report.getCourseId(), userId);
        return KnowledgeGapReportVO.from(
                report,
                course.getCourseName(),
                knowledgeGapItemRepository.countByReportId(report.getId()));
    }

    public List<KnowledgeGapItemVO> listItems(Long reportId, Long userId) {
        KnowledgeGapReport report = getOwnedReport(reportId, userId);
        courseService.getOwnedCourse(report.getCourseId(), userId);
        List<KnowledgeGapItem> items = knowledgeGapItemRepository.findByReportIdOrderBySeverityLevelDescIdAsc(reportId);
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, KnowledgeItem> knowledgeById = knowledgeItemRepository.findAllById(
                        items.stream().map(KnowledgeGapItem::getKnowledgeItemId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, Function.identity()));
        Map<Long, Course> courseById = courseRepository.findAllById(
                        items.stream().map(KnowledgeGapItem::getSourceCourseId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, CourseRelation> relationById = courseRelationRepository.findAllById(
                        items.stream()
                                .map(KnowledgeGapItem::getRelatedCourseRelationId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(CourseRelation::getId, Function.identity()));
        Map<Long, UserKnowledgeStatus> masteryByItemId = loadMastery(userId, knowledgeById.values());
        Map<Long, ExamKnowledgeStatVO> statByItemId = loadExamStatsForCourseIds(
                courseById.keySet(), userId);

        return items.stream()
                .map(item -> {
                    KnowledgeItem knowledge = knowledgeById.get(item.getKnowledgeItemId());
                    Course sourceCourse = courseById.get(item.getSourceCourseId());
                    CourseRelation relation = item.getRelatedCourseRelationId() == null
                            ? null
                            : relationById.get(item.getRelatedCourseRelationId());
                    UserKnowledgeStatus mastery = masteryByItemId.get(item.getKnowledgeItemId());
                    ExamKnowledgeStatVO stat = statByItemId.get(item.getKnowledgeItemId());
                    return KnowledgeGapItemVO.from(
                            item,
                            knowledge == null ? "未知知识点" : knowledge.getTitle(),
                            knowledge == null ? null : knowledge.getItemType(),
                            sourceCourse == null ? "未知课程" : sourceCourse.getCourseName(),
                            relation == null ? null : relation.getRelationType(),
                            mastery == null ? "UNKNOWN" : mastery.getMasteryStatus(),
                            mastery == null ? null : mastery.getMasteryScore(),
                            stat == null ? 0 : stat.getQuestionCount(),
                            stat == null ? BigDecimal.ZERO : stat.getTotalScore(),
                            stat == null ? null : stat.getLatestExamYear());
                })
                .collect(Collectors.toList());
    }

    private KnowledgeGapReport getOwnedReport(Long reportId, Long userId) {
        return knowledgeGapReportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new BusinessException("知识缺口报告不存在或无权访问"));
    }

    private List<SourceCourse> buildSourceCourses(Course course, Long userId, boolean includePrerequisites) {
        List<SourceCourse> result = new ArrayList<>();
        result.add(new SourceCourse(course, null));
        if (!includePrerequisites) {
            return result;
        }
        for (CourseRelation relation : courseRelationRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId())) {
            if (!"PREREQUISITE".equals(relation.getRelationType())) {
                continue;
            }
            Course prerequisite = courseService.getOwnedCourse(relation.getRelatedCourseId(), userId);
            result.add(new SourceCourse(prerequisite, relation));
        }
        return result;
    }

    private Map<Long, UserKnowledgeStatus> loadMastery(Long userId, Collection<KnowledgeItem> items) {
        if (items.isEmpty()) {
            return Collections.emptyMap();
        }
        return userKnowledgeStatusRepository.findByUserIdAndKnowledgeItemIdIn(
                        userId,
                        items.stream().map(KnowledgeItem::getId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserKnowledgeStatus::getKnowledgeItemId, Function.identity()));
    }

    private Map<Long, ExamKnowledgeStatVO> loadExamStats(List<SourceCourse> sourceCourses, Long userId) {
        return loadExamStatsForCourseIds(
                sourceCourses.stream().map(source -> source.course.getId()).collect(Collectors.toSet()),
                userId);
    }

    private Map<Long, ExamKnowledgeStatVO> loadExamStatsForCourseIds(Set<Long> courseIds, Long userId) {
        Map<Long, ExamKnowledgeStatVO> result = new LinkedHashMap<>();
        for (Long courseId : courseIds) {
            for (ExamKnowledgeStatVO stat : examQuestionService.listKnowledgeStats(courseId, userId, null, null, null)) {
                result.put(stat.getKnowledgeItemId(), stat);
            }
        }
        return result;
    }

    private KnowledgeGapItem buildGapItem(
            KnowledgeItem knowledgeItem,
            SourceCourse source,
            UserKnowledgeStatus mastery,
            ExamKnowledgeStatVO stat) {
        String masteryStatus = mastery == null ? "UNKNOWN" : mastery.getMasteryStatus();
        BigDecimal masteryScore = mastery == null ? null : mastery.getMasteryScore();
        long questionCount = stat == null ? 0 : stat.getQuestionCount();
        boolean sourceIsPrerequisite = source != null && source.relation != null;
        if (!shouldCreateGap(masteryStatus, masteryScore, questionCount, sourceIsPrerequisite)) {
            return null;
        }
        KnowledgeGapItem gap = new KnowledgeGapItem();
        gap.setKnowledgeItemId(knowledgeItem.getId());
        gap.setSourceCourseId(knowledgeItem.getCourseId());
        gap.setRelatedCourseRelationId(source == null || source.relation == null ? null : source.relation.getId());
        gap.setGapType(resolveGapType(masteryStatus, masteryScore, questionCount, sourceIsPrerequisite));
        gap.setSeverityLevel(resolveSeverity(masteryStatus, masteryScore, questionCount, sourceIsPrerequisite));
        gap.setReason(renderReason(masteryStatus, masteryScore, questionCount, stat, sourceIsPrerequisite));
        gap.setSuggestion(renderSuggestion(gap.getGapType(), sourceIsPrerequisite));
        return gap;
    }

    private boolean shouldCreateGap(
            String masteryStatus, BigDecimal masteryScore, long questionCount, boolean sourceIsPrerequisite) {
        if ("MASTERED".equals(masteryStatus)
                && (masteryScore == null || masteryScore.compareTo(new BigDecimal("85")) >= 0)
                && questionCount == 0) {
            return false;
        }
        if ("WEAK".equals(masteryStatus) || "NEED_REVIEW".equals(masteryStatus)) {
            return true;
        }
        if (masteryScore != null && masteryScore.compareTo(new BigDecimal("80")) < 0) {
            return true;
        }
        return questionCount > 0 || "UNKNOWN".equals(masteryStatus) || sourceIsPrerequisite;
    }

    private String resolveGapType(
            String masteryStatus, BigDecimal masteryScore, long questionCount, boolean sourceIsPrerequisite) {
        if ("WEAK".equals(masteryStatus)
                || (masteryScore != null && masteryScore.compareTo(new BigDecimal("60")) < 0)) {
            return "WEAK_MASTERY";
        }
        if ("NEED_REVIEW".equals(masteryStatus)) {
            return "NEED_REVIEW";
        }
        if (questionCount >= 2) {
            return "HIGH_FREQUENCY";
        }
        if (sourceIsPrerequisite) {
            return "PREREQUISITE_GAP";
        }
        return "UNASSESSED";
    }

    private int resolveSeverity(
            String masteryStatus, BigDecimal masteryScore, long questionCount, boolean sourceIsPrerequisite) {
        int severity = 1;
        if ("WEAK".equals(masteryStatus)) {
            severity += 3;
        } else if ("NEED_REVIEW".equals(masteryStatus)) {
            severity += 2;
        } else if ("UNKNOWN".equals(masteryStatus) || "LEARNING".equals(masteryStatus)) {
            severity += 1;
        }
        if (masteryScore != null) {
            if (masteryScore.compareTo(new BigDecimal("60")) < 0) {
                severity += 2;
            } else if (masteryScore.compareTo(new BigDecimal("80")) < 0) {
                severity += 1;
            }
        }
        if (questionCount >= 3) {
            severity += 2;
        } else if (questionCount > 0) {
            severity += 1;
        }
        if (sourceIsPrerequisite) {
            severity += 1;
        }
        return Math.min(5, severity);
    }

    private String renderReason(
            String masteryStatus,
            BigDecimal masteryScore,
            long questionCount,
            ExamKnowledgeStatVO stat,
            boolean sourceIsPrerequisite) {
        List<String> reasons = new ArrayList<>();
        reasons.add("掌握状态为 " + masteryStatus);
        if (masteryScore != null) {
            reasons.add("掌握分数 " + masteryScore);
        }
        if (questionCount > 0) {
            reasons.add("真题命中 " + questionCount + " 次"
                    + (stat == null || stat.getLatestExamYear() == null
                    ? ""
                    : "，最近出现在 " + stat.getLatestExamYear() + " 年"));
        }
        if (sourceIsPrerequisite) {
            reasons.add("来自前置课程，可能影响当前课程理解");
        }
        return String.join("；", reasons);
    }

    private String renderSuggestion(String gapType, boolean sourceIsPrerequisite) {
        if ("HIGH_FREQUENCY".equals(gapType)) {
            return "优先复习该知识点，并结合已映射真题进行专项练习。";
        }
        if ("WEAK_MASTERY".equals(gapType)) {
            return "先回看来源资料和章节总结，再用例题或真题验证掌握程度。";
        }
        if ("NEED_REVIEW".equals(gapType)) {
            return "安排一次短复习，补充备注中的疑点并更新掌握分数。";
        }
        if (sourceIsPrerequisite) {
            return "先补齐该前置知识，再继续当前课程相关章节。";
        }
        return "先做一次自测或人工评估，确认是否需要进一步复习。";
    }

    private String renderSummary(List<KnowledgeGapItem> gapItems, boolean includePrerequisites) {
        long severeCount = gapItems.stream().filter(item -> item.getSeverityLevel() >= 4).count();
        long prerequisiteCount = gapItems.stream().filter(item -> item.getRelatedCourseRelationId() != null).count();
        return "本次识别出 " + gapItems.size() + " 个知识缺口，其中高优先级 "
                + severeCount + " 个"
                + (includePrerequisites ? "，前置课程相关 " + prerequisiteCount + " 个。" : "。");
    }

    private String normalizeReportName(String value, String courseName) {
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return courseName + " 知识缺口报告 " + LocalDateTime.now().format(REPORT_TIME_FORMAT);
    }

    private static class SourceCourse {
        private final Course course;
        private final CourseRelation relation;

        private SourceCourse(Course course, CourseRelation relation) {
            this.course = course;
            this.relation = relation;
        }
    }
}

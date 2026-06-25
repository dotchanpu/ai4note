package com.example.coursekb.service;

import com.example.coursekb.entity.Chapter;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.ExamQuestionKnowledgeMapRepository;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExamKnowledgeTrendVO;
import com.example.coursekb.vo.ExamQuestionPageVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 负责真题查询类能力，例如筛选、排序、分页以及派生统计。
 */
@Service
public class ExamQuestionQueryService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 100;

    private final CourseService courseService;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final ChapterRepository chapterRepository;
    private final ExamQuestionAssembler examQuestionAssembler;
    private final ExamQuestionNormalizer examQuestionNormalizer;

    public ExamQuestionQueryService(
            CourseService courseService,
            ExamQuestionRepository examQuestionRepository,
            ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            ChapterRepository chapterRepository,
            ExamQuestionAssembler examQuestionAssembler,
            ExamQuestionNormalizer examQuestionNormalizer) {
        this.courseService = courseService;
        this.examQuestionRepository = examQuestionRepository;
        this.examQuestionKnowledgeMapRepository = examQuestionKnowledgeMapRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.chapterRepository = chapterRepository;
        this.examQuestionAssembler = examQuestionAssembler;
        this.examQuestionNormalizer = examQuestionNormalizer;
    }

    public ExamQuestionPageVO listQuestions(
            Long courseId,
            Long userId,
            Integer year,
            List<Long> chapterIds,
            String questionType,
            List<Long> materialIds,
            Integer page,
            Integer size) {
        courseService.getOwnedCourse(courseId, userId);
        List<ExamQuestion> questions = filterQuestions(courseId, year, chapterIds, questionType, materialIds);
        int safePage = normalizePage(page);
        int safeSize = normalizePageSize(size);
        long total = questions.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total * 1.0 / safeSize);
        int fromIndex = Math.min((safePage - 1) * safeSize, questions.size());
        int toIndex = Math.min(fromIndex + safeSize, questions.size());
        return ExamQuestionPageVO.of(
                examQuestionAssembler.toQuestionVOs(questions.subList(fromIndex, toIndex)),
                total,
                safePage,
                safeSize,
                totalPages);
    }

    public List<ExamKnowledgeStatVO> listKnowledgeStats(
            Long courseId,
            Long userId,
            Integer year,
            List<Long> chapterIds,
            String questionType,
            List<Long> materialIds) {
        courseService.getOwnedCourse(courseId, userId);
        List<ExamQuestion> filteredQuestions = filterQuestions(courseId, year, chapterIds, questionType, materialIds);
        if (filteredQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ExamQuestion> questionById = filteredQuestions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
        List<ExamQuestionKnowledgeMap> mappings = examQuestionKnowledgeMapRepository
                .findByExamQuestionIdInOrderByIdAsc(questionById.keySet());
        if (mappings.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, KnowledgeItem> knowledgeById = knowledgeItemRepository.findAllById(
                        mappings.stream()
                                .map(ExamQuestionKnowledgeMap::getKnowledgeItemId)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, Function.identity()));
        Map<Long, String> chapterTitleById = chapterRepository.findAllById(
                        knowledgeById.values().stream()
                                .map(KnowledgeItem::getChapterId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getChapterTitle));

        Map<Long, StatAccumulator> stats = new LinkedHashMap<>();
        for (ExamQuestionKnowledgeMap mapping : mappings) {
            ExamQuestion question = questionById.get(mapping.getExamQuestionId());
            KnowledgeItem knowledgeItem = knowledgeById.get(mapping.getKnowledgeItemId());
            if (question == null || knowledgeItem == null) {
                continue;
            }
            StatAccumulator accumulator = stats.computeIfAbsent(knowledgeItem.getId(), ignored -> {
                StatAccumulator created = new StatAccumulator();
                created.knowledgeItem = knowledgeItem;
                created.chapterTitle = knowledgeItem.getChapterId() == null
                        ? null
                        : chapterTitleById.get(knowledgeItem.getChapterId());
                created.totalScore = BigDecimal.ZERO;
                created.latestExamYear = question.getExamYear();
                return created;
            });
            accumulator.questionCount++;
            accumulator.totalScore = accumulator.totalScore.add(
                    question.getScore() == null ? BigDecimal.ZERO : question.getScore());
            accumulator.latestExamYear = maxYear(accumulator.latestExamYear, question.getExamYear());
        }

        return stats.values().stream()
                .map(accumulator -> {
                    ExamKnowledgeStatVO result = new ExamKnowledgeStatVO();
                    result.setKnowledgeItemId(accumulator.knowledgeItem.getId());
                    result.setKnowledgeTitle(accumulator.knowledgeItem.getTitle());
                    result.setKnowledgeItemType(accumulator.knowledgeItem.getItemType());
                    result.setChapterTitle(accumulator.chapterTitle);
                    result.setQuestionCount(accumulator.questionCount);
                    result.setTotalScore(accumulator.totalScore);
                    result.setLatestExamYear(accumulator.latestExamYear);
                    return result;
                })
                .sorted(Comparator.comparingLong(ExamKnowledgeStatVO::getQuestionCount)
                        .reversed()
                        .thenComparing(ExamKnowledgeStatVO::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExamKnowledgeStatVO::getKnowledgeItemId))
                .collect(Collectors.toList());
    }

    public List<ExamKnowledgeTrendVO> listKnowledgeTrends(
            Long courseId,
            Long userId,
            List<Long> chapterIds,
            String questionType,
            List<Long> materialIds) {
        courseService.getOwnedCourse(courseId, userId);
        List<ExamQuestion> filteredQuestions = filterQuestions(courseId, null, chapterIds, questionType, materialIds)
                .stream()
                .filter(question -> question.getExamYear() != null)
                .collect(Collectors.toList());
        if (filteredQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ExamQuestion> questionById = filteredQuestions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
        List<ExamQuestionKnowledgeMap> mappings = examQuestionKnowledgeMapRepository
                .findByExamQuestionIdInOrderByIdAsc(questionById.keySet());
        if (mappings.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, KnowledgeItem> knowledgeById = knowledgeItemRepository.findAllById(
                        mappings.stream()
                                .map(ExamQuestionKnowledgeMap::getKnowledgeItemId)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(KnowledgeItem::getId, Function.identity()));
        Map<Long, String> chapterTitleById = chapterRepository.findAllById(
                        knowledgeById.values().stream()
                                .map(KnowledgeItem::getChapterId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Chapter::getChapterTitle));

        Map<Long, TrendAccumulator> trends = new LinkedHashMap<>();
        for (ExamQuestionKnowledgeMap mapping : mappings) {
            ExamQuestion question = questionById.get(mapping.getExamQuestionId());
            KnowledgeItem knowledgeItem = knowledgeById.get(mapping.getKnowledgeItemId());
            if (question == null || knowledgeItem == null || question.getExamYear() == null) {
                continue;
            }
            TrendAccumulator accumulator = trends.computeIfAbsent(knowledgeItem.getId(), ignored -> {
                TrendAccumulator created = new TrendAccumulator();
                created.knowledgeItem = knowledgeItem;
                created.chapterTitle = knowledgeItem.getChapterId() == null
                        ? null
                        : chapterTitleById.get(knowledgeItem.getChapterId());
                return created;
            });
            YearAccumulator year = accumulator.yearly.computeIfAbsent(question.getExamYear(), ignored -> new YearAccumulator());
            year.questionCount++;
            year.totalScore = year.totalScore.add(question.getScore() == null ? BigDecimal.ZERO : question.getScore());
            accumulator.totalQuestionCount++;
            accumulator.totalScore = accumulator.totalScore.add(question.getScore() == null ? BigDecimal.ZERO : question.getScore());
        }

        return trends.values().stream()
                .map(accumulator -> {
                    ExamKnowledgeTrendVO result = new ExamKnowledgeTrendVO();
                    result.setKnowledgeItemId(accumulator.knowledgeItem.getId());
                    result.setKnowledgeTitle(accumulator.knowledgeItem.getTitle());
                    result.setKnowledgeItemType(accumulator.knowledgeItem.getItemType());
                    result.setChapterTitle(accumulator.chapterTitle);
                    result.setTotalQuestionCount(accumulator.totalQuestionCount);
                    result.setTotalScore(accumulator.totalScore);
                    result.setYearlyStats(accumulator.yearly.entrySet().stream()
                            .sorted(Map.Entry.<Integer, YearAccumulator>comparingByKey().reversed())
                            .map(entry -> new ExamKnowledgeTrendVO.YearStat(
                                    entry.getKey(),
                                    entry.getValue().questionCount,
                                    entry.getValue().totalScore))
                            .collect(Collectors.toList()));
                    return result;
                })
                .sorted(Comparator.comparingLong(ExamKnowledgeTrendVO::getTotalQuestionCount)
                        .reversed()
                        .thenComparing(ExamKnowledgeTrendVO::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExamKnowledgeTrendVO::getKnowledgeItemId))
                .collect(Collectors.toList());
    }

    private List<ExamQuestion> filterQuestions(
            Long courseId, Integer year, List<Long> chapterIds, String questionType, List<Long> materialIds) {
        String normalizedQuestionType = examQuestionNormalizer.normalizeQuestionType(questionType);
        return examQuestionRepository.findByCourseIdOrderByExamYearDescIdDesc(courseId)
                .stream()
                .filter(question -> year == null || Objects.equals(year, question.getExamYear()))
                .filter(question -> chapterIds == null || chapterIds.isEmpty()
                        || (question.getChapterId() != null && chapterIds.contains(question.getChapterId())))
                .filter(question -> materialIds == null
                        || (question.getMaterialId() != null && materialIds.contains(question.getMaterialId())))
                .filter(question -> normalizedQuestionType == null
                        || normalizedQuestionType.equals(examQuestionNormalizer.normalizeQuestionType(question.getQuestionType())))
                .sorted(this::compareQuestions)
                .collect(Collectors.toList());
    }

    /**
     * 使用稳定的内存排序，保证同一组筛选条件下分页结果不会漂移。
     */
    private int compareQuestions(ExamQuestion left, ExamQuestion right) {
        int byYear = compareNullableIntegersDesc(left.getExamYear(), right.getExamYear());
        if (byYear != 0) {
            return byYear;
        }
        int byMaterial = compareNullableLongsDesc(left.getMaterialId(), right.getMaterialId());
        if (byMaterial != 0) {
            return byMaterial;
        }
        int byPage = compareNullableIntegersAsc(left.getSourcePage(), right.getSourcePage());
        if (byPage != 0) {
            return byPage;
        }
        int byQuestionNo = compareQuestionNos(left.getQuestionNo(), right.getQuestionNo());
        if (byQuestionNo != 0) {
            return byQuestionNo;
        }
        return compareNullableLongsAsc(left.getId(), right.getId());
    }

    private int compareQuestionNos(String left, String right) {
        QuestionNoSortKey leftKey = QuestionNoSortKey.from(examQuestionNormalizer.normalizeQuestionNo(left));
        QuestionNoSortKey rightKey = QuestionNoSortKey.from(examQuestionNormalizer.normalizeQuestionNo(right));
        if (leftKey.parsed != rightKey.parsed) {
            return leftKey.parsed ? -1 : 1;
        }
        if (!leftKey.parsed) {
            return 0;
        }
        int prefixCompare = leftKey.prefix.compareTo(rightKey.prefix);
        if (prefixCompare != 0) {
            return prefixCompare;
        }
        int length = Math.max(leftKey.numbers.size(), rightKey.numbers.size());
        for (int index = 0; index < length; index++) {
            int leftNumber = index < leftKey.numbers.size() ? leftKey.numbers.get(index) : -1;
            int rightNumber = index < rightKey.numbers.size() ? rightKey.numbers.get(index) : -1;
            int compare = Integer.compare(leftNumber, rightNumber);
            if (compare != 0) {
                return compare;
            }
        }
        return leftKey.suffix.compareTo(rightKey.suffix);
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private int compareNullableIntegersDesc(Integer left, Integer right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private int compareNullableIntegersAsc(Integer left, Integer right) {
        return Comparator.nullsLast(Comparator.<Integer>naturalOrder()).compare(left, right);
    }

    private int compareNullableLongsDesc(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private int compareNullableLongsAsc(Long left, Long right) {
        return Comparator.nullsLast(Comparator.<Long>naturalOrder()).compare(left, right);
    }

    private Integer maxYear(Integer current, Integer candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        return Math.max(current, candidate);
    }

    private static class StatAccumulator {
        private KnowledgeItem knowledgeItem;
        private String chapterTitle;
        private long questionCount;
        private BigDecimal totalScore;
        private Integer latestExamYear;
    }

    private static class TrendAccumulator {
        private KnowledgeItem knowledgeItem;
        private String chapterTitle;
        private long totalQuestionCount;
        private BigDecimal totalScore = BigDecimal.ZERO;
        private Map<Integer, YearAccumulator> yearly = new LinkedHashMap<>();
    }

    private static class YearAccumulator {
        private long questionCount;
        private BigDecimal totalScore = BigDecimal.ZERO;
    }

    private static class QuestionNoSortKey {
        private final boolean parsed;
        private final String prefix;
        private final List<Integer> numbers;
        private final String suffix;

        private QuestionNoSortKey(boolean parsed, String prefix, List<Integer> numbers, String suffix) {
            this.parsed = parsed;
            this.prefix = prefix;
            this.numbers = numbers;
            this.suffix = suffix;
        }

        private static QuestionNoSortKey from(String value) {
            String normalized = value == null ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
            if (normalized == null || normalized.isEmpty()) {
                return new QuestionNoSortKey(false, "", Collections.emptyList(), "");
            }
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("^([A-Z]?)(\\d+(?:[.-]\\d+)*)([A-Z]?)$").matcher(normalized);
            if (!matcher.matches()) {
                return new QuestionNoSortKey(false, "", Collections.emptyList(), "");
            }
            List<Integer> numbers = new ArrayList<>();
            for (String part : matcher.group(2).split("[.-]")) {
                numbers.add(Integer.parseInt(part));
            }
            return new QuestionNoSortKey(true, matcher.group(1), numbers, matcher.group(3));
        }
    }
}

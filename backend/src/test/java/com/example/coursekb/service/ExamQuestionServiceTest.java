package com.example.coursekb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.coursekb.dto.ExamKnowledgeMapRequest;
import com.example.coursekb.entity.ExamQuestion;
import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.ExamQuestionKnowledgeMapRepository;
import com.example.coursekb.mapper.ExamQuestionRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionPageVO;
import com.example.coursekb.vo.ExamQuestionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamQuestionServiceTest {
    @Mock
    private MaterialService materialService;
    @Mock
    private CourseService courseService;
    @Mock
    private TextChunkRepository textChunkRepository;
    @Mock
    private ExamQuestionRepository examQuestionRepository;
    @Mock
    private ExamQuestionKnowledgeMapRepository examQuestionKnowledgeMapRepository;
    @Mock
    private KnowledgeItemRepository knowledgeItemRepository;
    @Mock
    private MaterialFileRepository materialFileRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private DeepSeekService deepSeekService;

    private ExamQuestionService examQuestionService;

    @BeforeEach
    void setUp() {
        examQuestionService = new ExamQuestionService(
                materialService,
                courseService,
                textChunkRepository,
                examQuestionRepository,
                examQuestionKnowledgeMapRepository,
                knowledgeItemRepository,
                materialFileRepository,
                materialRepository,
                chapterRepository,
                deepSeekService,
                new ObjectMapper());
    }

    @Test
    void extractQuestionsRejectsNonExamMaterial() {
        Material material = buildMaterial(11L, 7L, "SLIDE");
        when(materialService.getOwnedMaterial(11L, 3L)).thenReturn(material);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> examQuestionService.extractQuestions(11L, 3L, false));

        assertEquals("Only EXAM materials support exam extraction", exception.getMessage());
    }

    @Test
    void extractQuestionsRejectsNonPdfExamMaterial() {
        Material material = buildMaterial(11L, 7L, "EXAM");
        com.example.coursekb.entity.MaterialFile file = new com.example.coursekb.entity.MaterialFile();
        file.setMaterialId(11L);
        file.setFileType("txt");
        when(materialService.getOwnedMaterial(11L, 3L)).thenReturn(material);
        when(materialFileRepository.findByMaterialId(11L)).thenReturn(Optional.of(file));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> examQuestionService.extractQuestions(11L, 3L, false));

        assertEquals("EXAM_PDF_ONLY", exception.getCode());
        assertEquals("Only PDF exam materials support extraction", exception.getMessage());
    }

    @Test
    void extractQuestionsRejectsUnparsedMaterialWithCode() {
        Material material = buildMaterial(11L, 7L, "EXAM");
        com.example.coursekb.entity.MaterialFile file = new com.example.coursekb.entity.MaterialFile();
        file.setMaterialId(11L);
        file.setFileType("pdf");
        when(materialService.getOwnedMaterial(11L, 3L)).thenReturn(material);
        when(materialFileRepository.findByMaterialId(11L)).thenReturn(Optional.of(file));
        when(textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(11L)).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> examQuestionService.extractQuestions(11L, 3L, false));

        assertEquals("MATERIAL_NOT_PARSED", exception.getCode());
        assertEquals("Material must be parsed before exam extraction", exception.getMessage());
    }

    @Test
    void extractQuestionsRequiresOverwriteWhenQuestionsAlreadyExist() {
        Material material = buildMaterial(11L, 7L, "EXAM");
        com.example.coursekb.entity.MaterialFile file = new com.example.coursekb.entity.MaterialFile();
        file.setMaterialId(11L);
        file.setFileType("pdf");
        when(materialService.getOwnedMaterial(11L, 3L)).thenReturn(material);
        when(materialFileRepository.findByMaterialId(11L)).thenReturn(Optional.of(file));
        when(textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(11L))
                .thenReturn(Collections.singletonList(buildChunk(11L, 1, "sample")));
        when(examQuestionRepository.countByMaterialId(11L)).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> examQuestionService.extractQuestions(11L, 3L, false));

        assertEquals("EXAM_EXTRACTION_EXISTS", exception.getCode());
        verify(deepSeekService, never()).generateJson(any(Long.class), any(Long.class), any(String.class), any(String.class), any(String.class), any(Integer.class));
    }

    @Test
    void extractQuestionsAcceptsMarkdownWrappedJson() {
        Material material = buildMaterial(11L, 7L, "EXAM");
        com.example.coursekb.entity.MaterialFile file = new com.example.coursekb.entity.MaterialFile();
        file.setMaterialId(11L);
        file.setFileType("pdf");
        AtomicReference<List<ExamQuestion>> savedQuestions = new AtomicReference<>();
        when(materialService.getOwnedMaterial(11L, 3L)).thenReturn(material);
        when(materialFileRepository.findByMaterialId(11L)).thenReturn(Optional.of(file));
        when(textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(11L))
                .thenReturn(Collections.singletonList(buildChunk(11L, 1, "sample exam content")));
        when(examQuestionRepository.countByMaterialId(11L)).thenReturn(0L);
        when(deepSeekService.generateJson(anyLong(), anyLong(), anyString(), anyString(), isNull(), anyInt()))
                .thenReturn("```json\n{\"questions\":[{\"questionNo\":\"1\",\"questionType\":\"short answer\",\"questionText\":\"Explain BFS\",\"sourcePage\":2}]}\n```");
        when(examQuestionRepository.saveAll(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ExamQuestion> questions = (List<ExamQuestion>) invocation.getArgument(0);
            savedQuestions.set(questions);
            return questions;
        });

        List<ExamQuestionVO> result = examQuestionService.extractQuestions(11L, 3L, false);

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getQuestionNo());
        assertEquals("简答题", result.get(0).getQuestionType());
        assertEquals("Explain BFS", result.get(0).getQuestionText());
        assertFalse(savedQuestions.get().isEmpty());
        assertEquals("简答题", savedQuestions.get().get(0).getQuestionType());
    }

    @Test
    void extractQuestionsAcceptsJsonEmbeddedInText() {
        Material material = buildMaterial(11L, 7L, "EXAM");
        com.example.coursekb.entity.MaterialFile file = new com.example.coursekb.entity.MaterialFile();
        file.setMaterialId(11L);
        file.setFileType("pdf");
        when(materialService.getOwnedMaterial(11L, 3L)).thenReturn(material);
        when(materialFileRepository.findByMaterialId(11L)).thenReturn(Optional.of(file));
        when(textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(11L))
                .thenReturn(Collections.singletonList(buildChunk(11L, 1, "sample exam content")));
        when(examQuestionRepository.countByMaterialId(11L)).thenReturn(0L);
        when(deepSeekService.generateJson(anyLong(), anyLong(), anyString(), anyString(), isNull(), anyInt()))
                .thenReturn("Here is the extraction result:\n[{\"questionText\":\"Define stack\",\"questionType\":\"other\"}]");
        when(examQuestionRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ExamQuestionVO> result = examQuestionService.extractQuestions(11L, 3L, false);

        assertEquals(1, result.size());
        assertEquals("Define stack", result.get(0).getQuestionText());
        assertEquals("其他", result.get(0).getQuestionType());
    }

    @Test
    void extractQuestionsAutoMapsKnowledgeItemsWhenAvailable() throws Exception {
        Material material = buildMaterial(11L, 7L, "EXAM");
        com.example.coursekb.entity.MaterialFile file = new com.example.coursekb.entity.MaterialFile();
        file.setMaterialId(11L);
        file.setFileType("pdf");
        KnowledgeItem knowledgeItem = new KnowledgeItem();
        java.lang.reflect.Field knowledgeIdField = KnowledgeItem.class.getDeclaredField("id");
        knowledgeIdField.setAccessible(true);
        knowledgeIdField.set(knowledgeItem, 8L);
        knowledgeItem.setCourseId(7L);
        knowledgeItem.setTitle("二叉树遍历");
        knowledgeItem.setItemType("METHOD");
        knowledgeItem.setContent("说明二叉树遍历方法");
        AtomicReference<ExamQuestionKnowledgeMap> savedMap = new AtomicReference<>();

        when(materialService.getOwnedMaterial(11L, 3L)).thenReturn(material);
        when(materialFileRepository.findByMaterialId(11L)).thenReturn(Optional.of(file));
        when(textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(11L))
                .thenReturn(Collections.singletonList(buildChunk(11L, 1, "sample exam content")));
        when(examQuestionRepository.countByMaterialId(11L)).thenReturn(0L);
        when(deepSeekService.generateJson(anyLong(), anyLong(), anyString(), anyString(), isNull(), anyInt()))
                .thenReturn(
                        "{\"questions\":[{\"questionNo\":\"1\",\"questionType\":\"short answer\",\"questionText\":\"Explain inorder traversal\",\"sourcePage\":2}]}",
                        "{\"mappings\":[{\"questionId\":101,\"knowledgeItemId\":8,\"confidenceScore\":91.5,\"reason\":\"题目直接考察二叉树中序遍历\"}]}");
        when(examQuestionRepository.saveAll(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ExamQuestion> questions = (List<ExamQuestion>) invocation.getArgument(0);
            java.lang.reflect.Field questionIdField = ExamQuestion.class.getDeclaredField("id");
            questionIdField.setAccessible(true);
            long id = 101L;
            for (ExamQuestion question : questions) {
                questionIdField.set(question, id++);
            }
            return questions;
        });
        when(knowledgeItemRepository.findByCourseIdOrderByImportanceLevelDescIdDesc(7L))
                .thenReturn(Collections.singletonList(knowledgeItem));
        when(chapterRepository.findAllById(anyCollection())).thenReturn(Collections.emptyList());
        when(examQuestionKnowledgeMapRepository.findByExamQuestionIdAndKnowledgeItemId(101L, 8L))
                .thenReturn(Optional.empty());
        when(examQuestionKnowledgeMapRepository.save(any(ExamQuestionKnowledgeMap.class))).thenAnswer(invocation -> {
            ExamQuestionKnowledgeMap map = invocation.getArgument(0);
            savedMap.set(map);
            return map;
        });
        when(examQuestionKnowledgeMapRepository.findByExamQuestionIdInOrderByIdAsc(anyCollection()))
                .thenAnswer(invocation -> {
                    if (savedMap.get() == null) {
                        return Collections.emptyList();
                    }
                    return Collections.singletonList(savedMap.get());
                });
        when(knowledgeItemRepository.findAllById(anyCollection()))
                .thenReturn(Collections.singletonList(knowledgeItem));
        when(materialRepository.findAllById(anyCollection()))
                .thenReturn(Collections.singletonList(material));

        List<ExamQuestionVO> result = examQuestionService.extractQuestions(11L, 3L, false);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getMappings().size());
        assertEquals("AI", result.get(0).getMappings().get(0).getMatchSource());
        assertEquals("二叉树遍历", result.get(0).getMappings().get(0).getKnowledgeTitle());
        assertEquals(new BigDecimal("91.5"), savedMap.get().getConfidenceScore());
    }

    @Test
    void saveKnowledgeMapUpdatesExistingRelation() {
        ExamQuestion question = new ExamQuestion();
        question.setCourseId(7L);
        KnowledgeItem knowledgeItem = new KnowledgeItem();
        try {
            java.lang.reflect.Field idField = KnowledgeItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(knowledgeItem, 8L);
        } catch (ReflectiveOperationException ignored) {
        }
        knowledgeItem.setCourseId(7L);
        knowledgeItem.setTitle("二叉树遍历");
        knowledgeItem.setItemType("METHOD");
        ExamQuestionKnowledgeMap existing = new ExamQuestionKnowledgeMap();
        existing.setExamQuestionId(5L);
        existing.setKnowledgeItemId(8L);

        when(examQuestionRepository.findById(5L)).thenReturn(Optional.of(question));
        when(knowledgeItemRepository.findByIdAndCourseId(8L, 7L)).thenReturn(Optional.of(knowledgeItem));
        when(examQuestionKnowledgeMapRepository.findByExamQuestionIdAndKnowledgeItemId(5L, 8L))
                .thenReturn(Optional.of(existing));
        when(examQuestionKnowledgeMapRepository.save(existing)).thenReturn(existing);

        ExamKnowledgeMapRequest request = new ExamKnowledgeMapRequest();
        request.setKnowledgeItemId(8L);
        request.setMatchSource("manual");
        request.setConfidenceScore(new BigDecimal("95.5"));
        request.setReason("人工确认");

        ExamQuestionKnowledgeMapVO result = examQuestionService.saveKnowledgeMap(5L, 3L, request);

        assertEquals("MANUAL", result.getMatchSource());
        assertEquals("二叉树遍历", result.getKnowledgeTitle());
        assertEquals(new BigDecimal("95.5"), existing.getConfidenceScore());
    }

    @Test
    void listKnowledgeStatsAggregatesQuestionCountAndScore() throws Exception {
        ExamQuestion question1 = buildQuestion(1L, 7L, 11L, "CHOOSE", new BigDecimal("10"), 2024);
        ExamQuestion question2 = buildQuestion(2L, 7L, 11L, "CHOOSE", new BigDecimal("5"), 2025);
        ExamQuestionKnowledgeMap map1 = new ExamQuestionKnowledgeMap();
        map1.setExamQuestionId(1L);
        map1.setKnowledgeItemId(8L);
        ExamQuestionKnowledgeMap map2 = new ExamQuestionKnowledgeMap();
        map2.setExamQuestionId(2L);
        map2.setKnowledgeItemId(8L);
        KnowledgeItem knowledgeItem = new KnowledgeItem();
        java.lang.reflect.Field idField = KnowledgeItem.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(knowledgeItem, 8L);
        knowledgeItem.setTitle("二叉树遍历");
        knowledgeItem.setItemType("METHOD");

        when(examQuestionRepository.findByCourseIdOrderByExamYearDescIdDesc(7L))
                .thenReturn(Arrays.asList(question2, question1));
        when(examQuestionKnowledgeMapRepository.findByExamQuestionIdInOrderByIdAsc(anyCollection()))
                .thenReturn(Arrays.asList(map1, map2));
        when(knowledgeItemRepository.findAllById(anyCollection()))
                .thenReturn(Collections.singletonList(knowledgeItem));
        when(chapterRepository.findAllById(anyCollection()))
                .thenReturn(Collections.emptyList());

        List<ExamKnowledgeStatVO> stats = examQuestionService.listKnowledgeStats(7L, 3L, null, null, null);

        assertEquals(1, stats.size());
        assertEquals(2L, stats.get(0).getQuestionCount());
        assertEquals(new BigDecimal("15"), stats.get(0).getTotalScore());
        assertEquals(Integer.valueOf(2025), stats.get(0).getLatestExamYear());
    }

    @Test
    void listQuestionsReturnsStablePagedResultsWithNormalizedValues() throws Exception {
        ExamQuestion question1 = buildQuestion(1L, 7L, 11L, "SHORT_ANSWER", new BigDecimal("10"), 2024);
        question1.setSourcePage(1);
        question1.setQuestionNo("2");
        ExamQuestion question2 = buildQuestion(2L, 7L, 11L, "CHOICE", new BigDecimal("8"), 2024);
        question2.setSourcePage(1);
        question2.setQuestionNo(" 第 1 题 ");
        ExamQuestion question3 = buildQuestion(3L, 7L, 10L, "DEFINITION", new BigDecimal("5"), 2024);
        question3.setSourcePage(1);
        question3.setQuestionNo("??");
        ExamQuestion question4 = buildQuestion(4L, 7L, 12L, "PROGRAMMING", new BigDecimal("15"), 2023);
        question4.setSourcePage(2);
        question4.setQuestionNo("3");

        when(examQuestionRepository.findByCourseIdOrderByExamYearDescIdDesc(7L))
                .thenReturn(Arrays.asList(question4, question3, question1, question2));
        when(examQuestionKnowledgeMapRepository.findByExamQuestionIdInOrderByIdAsc(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(knowledgeItemRepository.findAllById(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(materialRepository.findAllById(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(chapterRepository.findAllById(anyCollection()))
                .thenReturn(Collections.emptyList());

        ExamQuestionPageVO firstPage = examQuestionService.listQuestions(7L, 3L, null, null, null, null, 1, 2);
        ExamQuestionPageVO secondPage = examQuestionService.listQuestions(7L, 3L, null, null, null, null, 2, 2);

        assertEquals(4L, firstPage.getTotal());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(2, firstPage.getItems().size());
        assertEquals(Long.valueOf(2L), firstPage.getItems().get(0).getId());
        assertEquals("1", firstPage.getItems().get(0).getQuestionNo());
        assertEquals("单选题", firstPage.getItems().get(0).getQuestionType());
        assertEquals(Long.valueOf(1L), firstPage.getItems().get(1).getId());
        assertEquals("简答题", firstPage.getItems().get(1).getQuestionType());

        assertEquals(2, secondPage.getItems().size());
        assertEquals(Long.valueOf(3L), secondPage.getItems().get(0).getId());
        assertEquals("名词解释", secondPage.getItems().get(0).getQuestionType());
        assertNull(secondPage.getItems().get(0).getQuestionNo());
        assertEquals(Long.valueOf(4L), secondPage.getItems().get(1).getId());
        assertEquals("编程题", secondPage.getItems().get(1).getQuestionType());
    }

    @Test
    void listQuestionsFiltersByNormalizedQuestionType() throws Exception {
        ExamQuestion question1 = buildQuestion(1L, 7L, 11L, "SHORT_ANSWER", new BigDecimal("10"), 2024);
        ExamQuestion question2 = buildQuestion(2L, 7L, 11L, "CHOICE", new BigDecimal("8"), 2024);

        when(examQuestionRepository.findByCourseIdOrderByExamYearDescIdDesc(7L))
                .thenReturn(Arrays.asList(question1, question2));
        when(examQuestionKnowledgeMapRepository.findByExamQuestionIdInOrderByIdAsc(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(knowledgeItemRepository.findAllById(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(materialRepository.findAllById(anyCollection()))
                .thenReturn(Collections.emptyList());
        when(chapterRepository.findAllById(anyCollection()))
                .thenReturn(Collections.emptyList());

        ExamQuestionPageVO page = examQuestionService.listQuestions(7L, 3L, null, null, "简答题", null, 1, 12);

        assertEquals(1L, page.getTotal());
        assertEquals(Long.valueOf(1L), page.getItems().get(0).getId());
        assertEquals("简答题", page.getItems().get(0).getQuestionType());
    }

    private Material buildMaterial(Long materialId, Long courseId, String materialType) {
        Material material = new Material();
        try {
            java.lang.reflect.Field idField = Material.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(material, materialId);
        } catch (ReflectiveOperationException ignored) {
        }
        material.setCourseId(courseId);
        material.setMaterialType(materialType);
        material.setTitle("sample");
        return material;
    }

    private TextChunk buildChunk(Long materialId, int index, String content) {
        TextChunk chunk = new TextChunk();
        chunk.setMaterialId(materialId);
        chunk.setChunkIndex(index);
        chunk.setPageNo(index);
        chunk.setContent(content);
        chunk.setWordCount(content.length());
        return chunk;
    }

    private ExamQuestion buildQuestion(
            Long id,
            Long courseId,
            Long materialId,
            String questionType,
            BigDecimal score,
            Integer examYear) throws Exception {
        ExamQuestion question = new ExamQuestion();
        java.lang.reflect.Field idField = ExamQuestion.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(question, id);
        question.setCourseId(courseId);
        question.setMaterialId(materialId);
        question.setQuestionType(questionType);
        question.setQuestionText("sample");
        question.setScore(score);
        question.setExamYear(examYear);
        return question;
    }
}

package com.example.coursekb.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.coursekb.dto.ReviewAssetGenerateRequest;
import com.example.coursekb.entity.AiGenerationTask;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.CourseRelation;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.mapper.AiGenerationTaskRepository;
import com.example.coursekb.mapper.CourseRelationRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.TeacherProfileRepository;
import com.example.coursekb.vo.MaterialVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewAssetServiceTest {
    @Mock
    private CourseService courseService;
    @Mock
    private TeacherProfileRepository teacherProfileRepository;
    @Mock
    private CourseRelationRepository courseRelationRepository;
    @Mock
    private KnowledgeItemRepository knowledgeItemRepository;
    @Mock
    private ExamQuestionService examQuestionService;
    @Mock
    private DeepSeekService deepSeekService;
    @Mock
    private AiGenerationTaskService aiGenerationTaskService;
    @Mock
    private AiGenerationTaskRepository aiGenerationTaskRepository;
    @Mock
    private MaterialService materialService;

    @TempDir
    private Path storageRoot;

    private ReviewAssetService reviewAssetService;

    @BeforeEach
    void setUp() {
        reviewAssetService = new ReviewAssetService(
                courseService,
                teacherProfileRepository,
                courseRelationRepository,
                knowledgeItemRepository,
                examQuestionService,
                deepSeekService,
                aiGenerationTaskService,
                aiGenerationTaskRepository,
                materialService,
                new ObjectMapper(),
                storageRoot.toString());
    }

    @Test
    void generateIncludesPrerequisiteKnowledgeWhenRequested() throws Exception {
        Course currentCourse = course(7L, 3L, "数据结构", "CS201");
        Course prerequisiteCourse = course(8L, 3L, "离散数学", "CS101");
        CourseRelation relation = relation(70L, 7L, 8L, "集合与图论基础");
        KnowledgeItem currentItem = knowledgeItem(101L, 7L, "图的遍历", "DFS 与 BFS 的应用");
        KnowledgeItem prerequisiteItem = knowledgeItem(201L, 8L, "集合运算", "交并补与笛卡尔积");
        ReviewAssetGenerateRequest request = request(true);
        AiGenerationTask task = task(900L, 3L, 7L);
        AtomicReference<String> userPrompt = new AtomicReference<>();

        when(courseService.getOwnedCourse(7L, 3L)).thenReturn(currentCourse);
        when(knowledgeItemRepository.findByCourseIdOrderByImportanceLevelDescIdDesc(7L))
                .thenReturn(Collections.singletonList(currentItem));
        when(courseRelationRepository.findByCourseIdOrderBySortOrderAscIdAsc(7L))
                .thenReturn(Collections.singletonList(relation));
        when(courseService.getOwnedCourse(8L, 3L)).thenReturn(prerequisiteCourse);
        when(knowledgeItemRepository.findByCourseIdOrderByImportanceLevelDescIdDesc(8L))
                .thenReturn(Collections.singletonList(prerequisiteItem));
        when(examQuestionService.listKnowledgeStats(7L, 3L, null, null, null, null))
                .thenReturn(Collections.emptyList());
        when(aiGenerationTaskService.createTask(eq(3L), eq(7L), eq("REVIEW_GENERATION"), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(task);
        when(aiGenerationTaskRepository.findById(900L)).thenReturn(Optional.of(task));
        when(deepSeekService.generateJson(eq(3L), eq(7L), anyString(), anyString(), eq("deepseek-v4-flash"), eq(8192)))
                .thenAnswer(invocation -> {
                    userPrompt.set(invocation.getArgument(3));
                    return "{\"title\":\"数据结构复习笔记\",\"markdown\":\"# 复习笔记\\n- 图的遍历\"}";
                });
        when(materialService.registerGeneratedFile(eq(3L), eq(7L), anyString(), eq("NOTE"), anyString(), any(Path.class)))
                .thenReturn(new MaterialVO());

        reviewAssetService.generate(7L, request);

        assertTrue(userPrompt.get().contains("Prerequisite course knowledge items"));
        assertTrue(userPrompt.get().contains("离散数学"));
        assertTrue(userPrompt.get().contains("集合运算"));
        assertTrue(userPrompt.get().contains("Target content balance: current course 75%-85%"));
    }

    @Test
    void generateSkipsPrerequisiteLookupWhenDisabled() throws Exception {
        Course currentCourse = course(7L, 3L, "数据结构", "CS201");
        KnowledgeItem currentItem = knowledgeItem(101L, 7L, "图的遍历", "DFS 与 BFS 的应用");
        ReviewAssetGenerateRequest request = request(false);
        AiGenerationTask task = task(901L, 3L, 7L);
        AtomicReference<String> userPrompt = new AtomicReference<>();

        when(courseService.getOwnedCourse(7L, 3L)).thenReturn(currentCourse);
        when(knowledgeItemRepository.findByCourseIdOrderByImportanceLevelDescIdDesc(7L))
                .thenReturn(Collections.singletonList(currentItem));
        when(examQuestionService.listKnowledgeStats(7L, 3L, null, null, null, null))
                .thenReturn(Collections.emptyList());
        when(aiGenerationTaskService.createTask(eq(3L), eq(7L), eq("REVIEW_GENERATION"), anyString(), isNull(), isNull(), isNull()))
                .thenReturn(task);
        when(aiGenerationTaskRepository.findById(901L)).thenReturn(Optional.of(task));
        when(deepSeekService.generateJson(eq(3L), eq(7L), anyString(), anyString(), eq("deepseek-v4-flash"), eq(8192)))
                .thenAnswer(invocation -> {
                    userPrompt.set(invocation.getArgument(3));
                    return "{\"title\":\"数据结构复习笔记\",\"markdown\":\"# 复习笔记\\n- 图的遍历\"}";
                });
        when(materialService.registerGeneratedFile(eq(3L), eq(7L), anyString(), eq("NOTE"), anyString(), any(Path.class)))
                .thenReturn(new MaterialVO());

        reviewAssetService.generate(7L, request);

        verify(courseRelationRepository, never()).findByCourseIdOrderBySortOrderAscIdAsc(anyLong());
        assertTrue(userPrompt.get().contains("includePrerequisites: false"));
        assertFalse(userPrompt.get().contains("集合运算"));
    }

    private ReviewAssetGenerateRequest request(boolean includePrerequisites) {
        ReviewAssetGenerateRequest request = new ReviewAssetGenerateRequest();
        request.setUserId(3L);
        request.setOutputType("REVIEW_NOTE");
        request.setDifficultyLevel("MEDIUM");
        request.setIncludePrerequisites(includePrerequisites);
        request.setModel("deepseek-v4-flash");
        return request;
    }

    private Course course(Long id, Long userId, String name, String code) {
        Course course = new Course();
        course.setId(id);
        course.setUserId(userId);
        course.setCourseName(name);
        course.setCourseCode(code);
        return course;
    }

    private CourseRelation relation(Long id, Long courseId, Long relatedCourseId, String reason) throws Exception {
        CourseRelation relation = new CourseRelation();
        setId(relation, id);
        relation.setCourseId(courseId);
        relation.setRelatedCourseId(relatedCourseId);
        relation.setRelationType("PREREQUISITE");
        relation.setReason(reason);
        relation.setSortOrder(0);
        return relation;
    }

    private KnowledgeItem knowledgeItem(Long id, Long courseId, String title, String content) throws Exception {
        KnowledgeItem item = new KnowledgeItem();
        setId(item, id);
        item.setCourseId(courseId);
        item.setTitle(title);
        item.setItemType("KEY_POINT");
        item.setContent(content);
        item.setImportanceLevel(5);
        return item;
    }

    private AiGenerationTask task(Long id, Long userId, Long courseId) throws Exception {
        AiGenerationTask task = new AiGenerationTask();
        setId(task, id);
        task.setUserId(userId);
        task.setCourseId(courseId);
        task.setTaskType("REVIEW_GENERATION");
        task.setStatus("SUCCESS");
        return task;
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}

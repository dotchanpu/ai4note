package com.example.coursekb.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.coursekb.dto.ExamKnowledgeMapRequest;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExamKnowledgeTrendVO;
import com.example.coursekb.vo.ExamQuestionKnowledgeMapVO;
import com.example.coursekb.vo.ExamQuestionPageVO;
import com.example.coursekb.vo.ExamQuestionVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamQuestionServiceFacadeTest {
    @Mock
    private ExamQuestionCommandService examQuestionCommandService;
    @Mock
    private ExamQuestionQueryService examQuestionQueryService;
    @Mock
    private ExamQuestionAnswerService examQuestionAnswerService;

    private ExamQuestionService examQuestionService;

    @BeforeEach
    void setUp() {
        examQuestionService = new ExamQuestionService(
                examQuestionCommandService, examQuestionQueryService, examQuestionAnswerService);
    }

    @Test
    void delegatesExtractQuestions() {
        List<ExamQuestionVO> expected = Collections.emptyList();
        when(examQuestionCommandService.extractQuestions(11L, 3L, true)).thenReturn(expected);

        assertSame(expected, examQuestionService.extractQuestions(11L, 3L, true));
        verify(examQuestionCommandService).extractQuestions(11L, 3L, true);
    }

    @Test
    void delegatesListQuestions() {
        ExamQuestionPageVO expected = ExamQuestionPageVO.of(Collections.emptyList(), 0L, 1, 12, 0);
        when(examQuestionQueryService.listQuestions(7L, 3L, 2024, Collections.singletonList(1L), "单选题",
                Collections.singletonList(9L), 1, 12)).thenReturn(expected);

        assertSame(expected, examQuestionService.listQuestions(
                7L, 3L, 2024, Collections.singletonList(1L), "单选题", Collections.singletonList(9L), 1, 12));
        verify(examQuestionQueryService).listQuestions(
                7L, 3L, 2024, Collections.singletonList(1L), "单选题", Collections.singletonList(9L), 1, 12);
    }

    @Test
    void delegatesSaveKnowledgeMap() {
        ExamKnowledgeMapRequest request = new ExamKnowledgeMapRequest();
        ExamQuestionKnowledgeMapVO expected = new ExamQuestionKnowledgeMapVO();
        when(examQuestionCommandService.saveKnowledgeMap(5L, 3L, request)).thenReturn(expected);

        assertSame(expected, examQuestionService.saveKnowledgeMap(5L, 3L, request));
        verify(examQuestionCommandService).saveKnowledgeMap(5L, 3L, request);
    }

    @Test
    void delegatesListKnowledgeStats() {
        List<ExamKnowledgeStatVO> expected = Collections.emptyList();
        when(examQuestionQueryService.listKnowledgeStats(7L, 3L, 2024, null, "简答题", null)).thenReturn(expected);

        assertSame(expected, examQuestionService.listKnowledgeStats(7L, 3L, 2024, null, "简答题", null));
        verify(examQuestionQueryService).listKnowledgeStats(7L, 3L, 2024, null, "简答题", null);
    }

    @Test
    void delegatesListKnowledgeTrends() {
        List<ExamKnowledgeTrendVO> expected = Collections.emptyList();
        when(examQuestionQueryService.listKnowledgeTrends(7L, 3L, null, "简答题", null)).thenReturn(expected);

        assertSame(expected, examQuestionService.listKnowledgeTrends(7L, 3L, null, "简答题", null));
        verify(examQuestionQueryService).listKnowledgeTrends(7L, 3L, null, "简答题", null);
    }

    @Test
    void delegatesGenerateAnswer() {
        ExamQuestionVO expected = new ExamQuestionVO();
        when(examQuestionAnswerService.generateAnswer(101L, 3L)).thenReturn(expected);

        assertSame(expected, examQuestionService.generateAnswer(101L, 3L));
        verify(examQuestionAnswerService).generateAnswer(101L, 3L);
    }

    @Test
    void delegatesGenerateBatchAnswers() {
        Map<String, Object> expected = Collections.singletonMap("total", 1);
        when(examQuestionAnswerService.generateBatchAnswers(7L, 3L)).thenReturn(expected);

        assertSame(expected, examQuestionService.generateBatchAnswers(7L, 3L));
        verify(examQuestionAnswerService).generateBatchAnswers(7L, 3L);
    }
}

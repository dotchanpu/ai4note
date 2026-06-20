package com.example.coursekb.mapper;

import com.example.coursekb.entity.ExamQuestionKnowledgeMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionKnowledgeMapRepository extends JpaRepository<ExamQuestionKnowledgeMap, Long> {
    void deleteByExamQuestionIdIn(Collection<Long> examQuestionIds);

    List<ExamQuestionKnowledgeMap> findByExamQuestionIdInOrderByIdAsc(Collection<Long> examQuestionIds);

    Optional<ExamQuestionKnowledgeMap> findByExamQuestionIdAndKnowledgeItemId(
            Long examQuestionId, Long knowledgeItemId);
}

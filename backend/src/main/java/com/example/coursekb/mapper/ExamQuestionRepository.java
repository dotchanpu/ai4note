package com.example.coursekb.mapper;

import com.example.coursekb.entity.ExamQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    long countByMaterialId(Long materialId);

    List<ExamQuestion> findByCourseIdOrderByExamYearDescIdDesc(Long courseId);

    List<ExamQuestion> findByMaterialIdOrderByIdAsc(Long materialId);

    Optional<ExamQuestion> findByIdAndCourseId(Long id, Long courseId);

    List<ExamQuestion> findByCourseIdAndAnswerTextIsNullOrderByIdAsc(Long courseId);

    long countByCourseIdAndAnswerTextIsNull(Long courseId);
}

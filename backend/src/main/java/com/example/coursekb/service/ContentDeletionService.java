package com.example.coursekb.service;

import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.Material;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.CourseRepository;
import com.example.coursekb.mapper.MaterialRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ContentDeletionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDeletionService.class);

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final MaterialRepository materialRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Path storageRoot;

    public ContentDeletionService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            MaterialRepository materialRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${ai4note.storage-root}") String storageRoot) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.materialRepository = materialRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional
    public void deleteMaterial(Long materialId, Long userId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException("资料不存在"));
        getOwnedCourse(material.getCourseId(), userId);

        List<String> filePaths = findMaterialFilePaths(materialId);
        deleteMaterialRecords(materialId);
        scheduleFileDeletion(filePaths);
    }

    @Transactional
    public void deleteChapter(Long courseId, Long chapterId, Long userId) {
        getOwnedCourse(courseId, userId);
        if (!chapterRepository.findByIdAndCourseId(chapterId, courseId).isPresent()) {
            throw new BusinessException("章节不存在");
        }

        jdbcTemplate.update("UPDATE material SET chapter_id = NULL WHERE chapter_id = ?", chapterId);
        jdbcTemplate.update("UPDATE knowledge_item SET chapter_id = NULL WHERE chapter_id = ?", chapterId);
        jdbcTemplate.update("UPDATE exam_question SET chapter_id = NULL WHERE chapter_id = ?", chapterId);
        jdbcTemplate.update("DELETE FROM chapter WHERE id = ? AND course_id = ?", chapterId, courseId);
    }

    @Transactional
    public void deleteCourse(Long courseId, Long userId) {
        getOwnedCourse(courseId, userId);
        List<String> filePaths = jdbcTemplate.queryForList(
                "SELECT mf.file_path FROM material_file mf "
                        + "JOIN material m ON m.id = mf.material_id WHERE m.course_id = ?",
                String.class,
                courseId);

        jdbcTemplate.update(
                "DELETE FROM ai_generation_task WHERE course_id = ? "
                        + "OR review_profile_id IN (SELECT id FROM review_generation_profile WHERE course_id = ?) "
                        + "OR teacher_profile_id IN (SELECT id FROM teacher_profile WHERE course_id = ?)",
                courseId, courseId, courseId);
        jdbcTemplate.update(
                "DELETE FROM knowledge_gap_item WHERE "
                        + "report_id IN (SELECT id FROM knowledge_gap_report WHERE course_id = ?) "
                        + "OR source_course_id = ? "
                        + "OR related_course_relation_id IN "
                        + "(SELECT id FROM course_relation WHERE course_id = ? OR prerequisite_course_id = ?) "
                        + "OR knowledge_item_id IN (SELECT id FROM knowledge_item WHERE course_id = ?)",
                courseId, courseId, courseId, courseId, courseId);
        jdbcTemplate.update("DELETE FROM knowledge_gap_report WHERE course_id = ?", courseId);
        jdbcTemplate.update(
                "DELETE FROM teacher_profile_evidence WHERE "
                        + "teacher_profile_id IN (SELECT id FROM teacher_profile WHERE course_id = ?) "
                        + "OR material_id IN (SELECT id FROM material WHERE course_id = ?)",
                courseId, courseId);
        jdbcTemplate.update("DELETE FROM review_generation_profile WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM teacher_profile WHERE course_id = ?", courseId);
        jdbcTemplate.update(
                "DELETE FROM exam_question_knowledge_map WHERE "
                        + "exam_question_id IN (SELECT id FROM exam_question WHERE course_id = ?) "
                        + "OR knowledge_item_id IN (SELECT id FROM knowledge_item WHERE course_id = ?)",
                courseId, courseId);
        jdbcTemplate.update(
                "DELETE FROM user_knowledge_status WHERE knowledge_item_id IN "
                        + "(SELECT id FROM knowledge_item WHERE course_id = ?)",
                courseId);
        jdbcTemplate.update("DELETE FROM exam_question WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM knowledge_item WHERE course_id = ?", courseId);
        jdbcTemplate.update(
                "DELETE mt FROM material_tag mt JOIN material m ON m.id = mt.material_id "
                        + "WHERE m.course_id = ?",
                courseId);
        jdbcTemplate.update(
                "DELETE tc FROM text_chunk tc JOIN material m ON m.id = tc.material_id "
                        + "WHERE m.course_id = ?",
                courseId);
        jdbcTemplate.update(
                "DELETE mf FROM material_file mf JOIN material m ON m.id = mf.material_id "
                        + "WHERE m.course_id = ?",
                courseId);
        jdbcTemplate.update(
                "DELETE FROM course_relation WHERE course_id = ? OR prerequisite_course_id = ?",
                courseId, courseId);
        jdbcTemplate.update("DELETE FROM search_record WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM export_record WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM material WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM chapter WHERE course_id = ?", courseId);
        jdbcTemplate.update("DELETE FROM course WHERE id = ?", courseId);

        scheduleFileDeletion(filePaths);
    }

    private Course getOwnedCourse(Long courseId, Long userId) {
        return courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new BusinessException("课程不存在或无权访问"));
    }

    private List<String> findMaterialFilePaths(Long materialId) {
        List<String> paths = jdbcTemplate.queryForList(
                "SELECT file_path FROM material_file WHERE material_id = ?",
                String.class,
                materialId);
        return paths == null ? Collections.emptyList() : paths;
    }

    private void deleteMaterialRecords(Long materialId) {
        jdbcTemplate.update(
                "DELETE FROM teacher_profile_evidence WHERE material_id = ?",
                materialId);
        jdbcTemplate.update(
                "DELETE FROM exam_question_knowledge_map WHERE "
                        + "exam_question_id IN (SELECT id FROM exam_question WHERE material_id = ?) "
                        + "OR knowledge_item_id IN (SELECT id FROM knowledge_item WHERE material_id = ?)",
                materialId, materialId);
        jdbcTemplate.update(
                "DELETE FROM knowledge_gap_item WHERE knowledge_item_id IN "
                        + "(SELECT id FROM knowledge_item WHERE material_id = ?)",
                materialId);
        jdbcTemplate.update(
                "DELETE FROM user_knowledge_status WHERE knowledge_item_id IN "
                        + "(SELECT id FROM knowledge_item WHERE material_id = ?)",
                materialId);
        jdbcTemplate.update("DELETE FROM exam_question WHERE material_id = ?", materialId);
        jdbcTemplate.update("DELETE FROM knowledge_item WHERE material_id = ?", materialId);
        jdbcTemplate.update("DELETE FROM material_tag WHERE material_id = ?", materialId);
        jdbcTemplate.update("DELETE FROM text_chunk WHERE material_id = ?", materialId);
        jdbcTemplate.update("DELETE FROM material_file WHERE material_id = ?", materialId);
        jdbcTemplate.update("DELETE FROM material WHERE id = ?", materialId);
    }

    private void scheduleFileDeletion(List<String> filePaths) {
        if (filePaths.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                filePaths.forEach(ContentDeletionService.this::deleteStoredFile);
            }
        });
    }

    private void deleteStoredFile(String relativePath) {
        Path path = storageRoot.resolve(relativePath).normalize();
        if (!path.startsWith(storageRoot)) {
            LOGGER.warn("Skipped deleting file outside storage root: {}", path);
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.error("Failed to delete stored file {}", path, exception);
        }
    }
}

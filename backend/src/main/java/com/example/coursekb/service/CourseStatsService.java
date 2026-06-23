package com.example.coursekb.service;

import com.example.coursekb.entity.Course;
import com.example.coursekb.vo.CourseStatsVO;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CourseStatsService {
    private final CourseService courseService;
    private final JdbcTemplate jdbcTemplate;

    public CourseStatsService(CourseService courseService, JdbcTemplate jdbcTemplate) {
        this.courseService = courseService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CourseStatsVO getStats(Long courseId, Long userId) {
        Course course = courseService.getOwnedCourse(courseId, userId);
        CourseStatsVO stats = new CourseStatsVO();
        stats.setCourseId(course.getId());
        stats.setCourseName(course.getCourseName());
        stats.setMaterialCount(count("SELECT COUNT(*) FROM material WHERE course_id = ?", courseId));
        stats.setParsedMaterialCount(count(
                "SELECT COUNT(DISTINCT material_id) FROM text_chunk WHERE material_id IN "
                        + "(SELECT id FROM material WHERE course_id = ?)",
                courseId));
        stats.setKnowledgeItemCount(count("SELECT COUNT(*) FROM knowledge_item WHERE course_id = ?", courseId));
        stats.setExamQuestionCount(count("SELECT COUNT(*) FROM exam_question WHERE course_id = ?", courseId));
        stats.setExamMappingCount(count(
                "SELECT COUNT(*) FROM exam_question_knowledge_map m "
                        + "JOIN exam_question q ON q.id = m.exam_question_id WHERE q.course_id = ?",
                courseId));
        stats.setExportCount(count("SELECT COUNT(*) FROM export_record WHERE course_id = ? AND user_id = ?",
                courseId, userId));
        stats.setMaterialTypeStats(loadMaterialTypeStats(courseId));
        return stats;
    }

    private List<CourseStatsVO.MaterialTypeStat> loadMaterialTypeStats(Long courseId) {
        return jdbcTemplate.query(
                "SELECT material_type, COUNT(*) AS count_value FROM material "
                        + "WHERE course_id = ? GROUP BY material_type ORDER BY count_value DESC, material_type ASC",
                (rs, rowNum) -> new CourseStatsVO.MaterialTypeStat(
                        rs.getString("material_type"),
                        rs.getLong("count_value")),
                courseId);
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}

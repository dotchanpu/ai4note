package com.example.coursekb.service;

import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.vo.MaterialSearchResultVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    private static final int MAX_RESULTS = 100;
    private static final int SNIPPET_LENGTH = 220;

    private final CourseService courseService;
    private final JdbcTemplate jdbcTemplate;

    public SearchService(CourseService courseService, JdbcTemplate jdbcTemplate) {
        this.courseService = courseService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public List<MaterialSearchResultVO> search(
            Long userId,
            Long courseId,
            String keyword,
            Long chapterId,
            String materialType,
            Boolean keyOnly) {
        courseService.getOwnedCourse(courseId, userId);
        String normalizedKeyword = normalizeKeyword(keyword);
        String keywordPattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";

        StringBuilder sql = new StringBuilder(
                "SELECT m.id AS material_id, m.course_id, m.chapter_id, "
                        + "c.chapter_no, c.chapter_title, m.title, m.material_type, "
                        + "m.summary, m.year, m.is_key, mf.original_name, mf.file_type, "
                        + "mf.file_size, "
                        + "(SELECT COUNT(*) FROM text_chunk count_tc WHERE count_tc.material_id = m.id) "
                        + "AS parsed_chunk_count, "
                        + "(SELECT match_tc.page_no FROM text_chunk match_tc "
                        + "WHERE match_tc.material_id = m.id AND LOWER(match_tc.content) LIKE ? "
                        + "ORDER BY match_tc.chunk_index LIMIT 1) AS matched_page_no, "
                        + "(SELECT match_tc.content FROM text_chunk match_tc "
                        + "WHERE match_tc.material_id = m.id AND LOWER(match_tc.content) LIKE ? "
                        + "ORDER BY match_tc.chunk_index LIMIT 1) AS matched_content "
                        + "FROM material m "
                        + "LEFT JOIN chapter c ON c.id = m.chapter_id "
                        + "LEFT JOIN material_file mf ON mf.material_id = m.id "
                        + "WHERE m.course_id = ? "
                        + "AND (LOWER(m.title) LIKE ? "
                        + "OR LOWER(COALESCE(m.summary, '')) LIKE ? "
                        + "OR EXISTS (SELECT 1 FROM text_chunk tc "
                        + "WHERE tc.material_id = m.id AND LOWER(tc.content) LIKE ?))");
        List<Object> parameters = new ArrayList<>();
        parameters.add(keywordPattern);
        parameters.add(keywordPattern);
        parameters.add(courseId);
        parameters.add(keywordPattern);
        parameters.add(keywordPattern);
        parameters.add(keywordPattern);

        if (chapterId != null) {
            sql.append(" AND m.chapter_id = ?");
            parameters.add(chapterId);
        }
        if (materialType != null && !materialType.trim().isEmpty()) {
            sql.append(" AND m.material_type = ?");
            parameters.add(materialType.trim().toUpperCase(Locale.ROOT));
        }
        if (Boolean.TRUE.equals(keyOnly)) {
            sql.append(" AND m.is_key = 1");
        }
        sql.append(" ORDER BY "
                + "CASE WHEN LOWER(m.title) LIKE ? THEN 0 "
                + "WHEN LOWER(COALESCE(m.summary, '')) LIKE ? THEN 1 ELSE 2 END, "
                + "m.upload_time DESC LIMIT ")
                .append(MAX_RESULTS);
        parameters.add(keywordPattern);
        parameters.add(keywordPattern);

        List<MaterialSearchResultVO> results = jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> mapResult(resultSet, normalizedKeyword),
                parameters.toArray());
        jdbcTemplate.update(
                "INSERT INTO search_record "
                        + "(user_id, course_id, keyword, search_type, result_count) "
                        + "VALUES (?, ?, ?, ?, ?)",
                userId,
                courseId,
                normalizedKeyword,
                "MATERIAL_FULLTEXT",
                results.size());
        return results;
    }

    private MaterialSearchResultVO mapResult(ResultSet resultSet, String keyword)
            throws SQLException {
        MaterialSearchResultVO result = new MaterialSearchResultVO();
        result.setMaterialId(resultSet.getLong("material_id"));
        result.setCourseId(resultSet.getLong("course_id"));
        result.setChapterId(nullableLong(resultSet, "chapter_id"));
        result.setChapterNo(resultSet.getString("chapter_no"));
        result.setChapterTitle(resultSet.getString("chapter_title"));
        result.setTitle(resultSet.getString("title"));
        result.setMaterialType(resultSet.getString("material_type"));
        result.setSummary(resultSet.getString("summary"));
        result.setYear(nullableInteger(resultSet, "year"));
        result.setKey(resultSet.getInt("is_key") != 0);
        result.setOriginalName(resultSet.getString("original_name"));
        result.setFileType(resultSet.getString("file_type"));
        result.setFileSize(nullableLong(resultSet, "file_size"));
        result.setParsedChunkCount(resultSet.getLong("parsed_chunk_count"));
        result.setMatchedPageNo(nullableInteger(resultSet, "matched_page_no"));

        String matchedContent = resultSet.getString("matched_content");
        String source;
        String sourceText;
        if (containsIgnoreCase(result.getTitle(), keyword)) {
            source = "TITLE";
            sourceText = result.getTitle();
        } else if (containsIgnoreCase(result.getSummary(), keyword)) {
            source = "SUMMARY";
            sourceText = result.getSummary();
        } else {
            source = "CONTENT";
            sourceText = matchedContent;
        }
        result.setMatchSource(source);
        result.setMatchedSnippet(createSnippet(sourceText, keyword));
        return result;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("请输入检索关键词");
        }
        String normalized = keyword.trim();
        if (normalized.length() > 255) {
            throw new BusinessException("检索关键词不能超过 255 个字符");
        }
        return normalized;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String createSnippet(String value, String keyword) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        String lowerValue = normalized.toLowerCase(Locale.ROOT);
        int matchIndex = lowerValue.indexOf(keyword.toLowerCase(Locale.ROOT));
        int start = matchIndex < 0 ? 0 : Math.max(0, matchIndex - SNIPPET_LENGTH / 3);
        int end = Math.min(normalized.length(), start + SNIPPET_LENGTH);
        if (end - start < SNIPPET_LENGTH && start > 0) {
            start = Math.max(0, end - SNIPPET_LENGTH);
        }
        return (start > 0 ? "…" : "")
                + normalized.substring(start, end)
                + (end < normalized.length() ? "…" : "");
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}

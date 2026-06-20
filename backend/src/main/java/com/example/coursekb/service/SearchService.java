package com.example.coursekb.service;

import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.vo.MaterialSearchResultVO;
import com.example.coursekb.vo.SearchRecordVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

        List<MaterialSearchResultVO> results = searchMaterials(
                courseId, normalizedKeyword, keywordPattern, chapterId, materialType, keyOnly);
        if (results.size() < MAX_RESULTS) {
            results.addAll(searchKnowledgeItems(
                    courseId,
                    normalizedKeyword,
                    keywordPattern,
                    chapterId,
                    materialType,
                    keyOnly,
                    MAX_RESULTS - results.size()));
        }
        jdbcTemplate.update(
                "INSERT INTO search_record "
                        + "(user_id, course_id, keyword, search_type, result_count) "
                        + "VALUES (?, ?, ?, ?, ?)",
                userId,
                courseId,
                normalizedKeyword,
                "UNIFIED_KNOWLEDGE",
                results.size());
        return results;
    }

    public List<SearchRecordVO> listRecords(Long userId, Long courseId) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT sr.id, sr.user_id, sr.course_id, c.course_name, sr.keyword, "
                        + "sr.search_type, sr.result_count, sr.search_time "
                        + "FROM search_record sr "
                        + "LEFT JOIN course c ON c.id = sr.course_id "
                        + "WHERE sr.user_id = ?");
        parameters.add(userId);
        if (courseId != null) {
            courseService.getOwnedCourse(courseId, userId);
            sql.append(" AND sr.course_id = ?");
            parameters.add(courseId);
        }
        sql.append(" ORDER BY sr.search_time DESC, sr.id DESC LIMIT 100");
        return jdbcTemplate.query(sql.toString(), this::mapSearchRecord, parameters.toArray());
    }

    private List<MaterialSearchResultVO> searchMaterials(
            Long courseId,
            String normalizedKeyword,
            String keywordPattern,
            Long chapterId,
            String materialType,
            Boolean keyOnly) {
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
                        + "ORDER BY match_tc.chunk_index LIMIT 1) AS matched_content, "
                        + "(SELECT t.tag_name FROM tag t JOIN material_tag mt ON mt.tag_id = t.id "
                        + "WHERE mt.material_id = m.id AND LOWER(t.tag_name) LIKE ? "
                        + "ORDER BY t.tag_name LIMIT 1) AS matched_tag "
                        + "FROM material m "
                        + "LEFT JOIN chapter c ON c.id = m.chapter_id "
                        + "LEFT JOIN material_file mf ON mf.material_id = m.id "
                        + "WHERE m.course_id = ? "
                        + "AND (LOWER(m.title) LIKE ? "
                        + "OR LOWER(COALESCE(m.summary, '')) LIKE ? "
                        + "OR EXISTS (SELECT 1 FROM text_chunk tc "
                        + "WHERE tc.material_id = m.id AND LOWER(tc.content) LIKE ?) "
                        + "OR EXISTS (SELECT 1 FROM material_tag mt JOIN tag t ON t.id = mt.tag_id "
                        + "WHERE mt.material_id = m.id AND LOWER(t.tag_name) LIKE ?))");
        List<Object> parameters = new ArrayList<>();
        parameters.add(keywordPattern);
        parameters.add(keywordPattern);
        parameters.add(keywordPattern);
        parameters.add(courseId);
        parameters.add(keywordPattern);
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

        return jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> mapResult(resultSet, normalizedKeyword),
                parameters.toArray());
    }

    private List<MaterialSearchResultVO> searchKnowledgeItems(
            Long courseId,
            String normalizedKeyword,
            String keywordPattern,
            Long chapterId,
            String materialType,
            Boolean keyOnly,
            int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT ki.id AS knowledge_item_id, ki.course_id, ki.material_id, "
                        + "ki.chapter_id, c.chapter_no, c.chapter_title, ki.title, "
                        + "ki.item_type, ki.content, ki.source_page, ki.importance_level, "
                        + "m.material_type, m.summary, m.year, m.is_key, "
                        + "mf.original_name, mf.file_type, mf.file_size, "
                        + "(SELECT COUNT(*) FROM text_chunk tc WHERE tc.material_id = m.id) "
                        + "AS parsed_chunk_count "
                        + "FROM knowledge_item ki "
                        + "LEFT JOIN material m ON m.id = ki.material_id "
                        + "LEFT JOIN chapter c ON c.id = ki.chapter_id "
                        + "LEFT JOIN material_file mf ON mf.material_id = m.id "
                        + "WHERE ki.course_id = ? "
                        + "AND (LOWER(ki.title) LIKE ? OR LOWER(ki.content) LIKE ?)");
        List<Object> parameters = new ArrayList<>();
        parameters.add(courseId);
        parameters.add(keywordPattern);
        parameters.add(keywordPattern);
        if (chapterId != null) {
            sql.append(" AND ki.chapter_id = ?");
            parameters.add(chapterId);
        }
        if (materialType != null && !materialType.trim().isEmpty()) {
            sql.append(" AND m.material_type = ?");
            parameters.add(materialType.trim().toUpperCase(Locale.ROOT));
        }
        if (Boolean.TRUE.equals(keyOnly)) {
            sql.append(" AND m.is_key = 1");
        }
        sql.append(" ORDER BY CASE WHEN LOWER(ki.title) LIKE ? THEN 0 ELSE 1 END, "
                + "ki.importance_level DESC, ki.id DESC LIMIT ").append(limit);
        parameters.add(keywordPattern);
        return jdbcTemplate.query(
                sql.toString(),
                (resultSet, rowNumber) -> mapKnowledgeResult(resultSet, normalizedKeyword),
                parameters.toArray());
    }

    private MaterialSearchResultVO mapResult(ResultSet resultSet, String keyword)
            throws SQLException {
        MaterialSearchResultVO result = new MaterialSearchResultVO();
        result.setResultType("MATERIAL");
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
        String matchedTag = resultSet.getString("matched_tag");
        String source;
        String sourceText;
        if (containsIgnoreCase(result.getTitle(), keyword)) {
            source = "TITLE";
            sourceText = result.getTitle();
        } else if (containsIgnoreCase(result.getSummary(), keyword)) {
            source = "SUMMARY";
            sourceText = result.getSummary();
        } else if (containsIgnoreCase(matchedTag, keyword)) {
            source = "TAG";
            sourceText = matchedTag;
        } else {
            source = "CONTENT";
            sourceText = matchedContent;
        }
        result.setMatchSource(source);
        result.setMatchedSnippet(createSnippet(sourceText, keyword));
        return result;
    }

    private SearchRecordVO mapSearchRecord(ResultSet resultSet, int rowNumber)
            throws SQLException {
        SearchRecordVO record = new SearchRecordVO();
        record.setId(resultSet.getLong("id"));
        record.setUserId(resultSet.getLong("user_id"));
        record.setCourseId(nullableLong(resultSet, "course_id"));
        record.setCourseName(resultSet.getString("course_name"));
        record.setKeyword(resultSet.getString("keyword"));
        record.setSearchType(resultSet.getString("search_type"));
        record.setResultCount(resultSet.getInt("result_count"));
        Timestamp searchTime = resultSet.getTimestamp("search_time");
        record.setSearchTime(searchTime == null ? null : searchTime.toLocalDateTime());
        return record;
    }

    private MaterialSearchResultVO mapKnowledgeResult(ResultSet resultSet, String keyword)
            throws SQLException {
        MaterialSearchResultVO result = new MaterialSearchResultVO();
        result.setResultType("KNOWLEDGE_ITEM");
        result.setKnowledgeItemId(resultSet.getLong("knowledge_item_id"));
        result.setCourseId(resultSet.getLong("course_id"));
        result.setMaterialId(nullableLong(resultSet, "material_id"));
        result.setChapterId(nullableLong(resultSet, "chapter_id"));
        result.setChapterNo(resultSet.getString("chapter_no"));
        result.setChapterTitle(resultSet.getString("chapter_title"));
        result.setTitle(resultSet.getString("title"));
        result.setItemType(resultSet.getString("item_type"));
        result.setImportanceLevel(resultSet.getInt("importance_level"));
        result.setMaterialType(resultSet.getString("material_type"));
        result.setSummary(resultSet.getString("summary"));
        result.setYear(nullableInteger(resultSet, "year"));
        result.setKey(resultSet.getInt("is_key") != 0);
        result.setOriginalName(resultSet.getString("original_name"));
        result.setFileType(resultSet.getString("file_type"));
        result.setFileSize(nullableLong(resultSet, "file_size"));
        result.setParsedChunkCount(resultSet.getLong("parsed_chunk_count"));
        result.setMatchedPageNo(nullableInteger(resultSet, "source_page"));
        String content = resultSet.getString("content");
        result.setMatchSource(containsIgnoreCase(result.getTitle(), keyword)
                ? "KNOWLEDGE_TITLE"
                : "KNOWLEDGE_CONTENT");
        result.setMatchedSnippet(createSnippet(
                containsIgnoreCase(result.getTitle(), keyword) ? result.getTitle() : content,
                keyword));
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

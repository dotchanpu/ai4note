package com.example.coursekb.service;

import com.example.coursekb.dto.ExportRequest;
import com.example.coursekb.entity.Chapter;
import com.example.coursekb.entity.Course;
import com.example.coursekb.entity.CourseRelation;
import com.example.coursekb.entity.ExportRecord;
import com.example.coursekb.entity.ExportTemplate;
import com.example.coursekb.entity.KnowledgeItem;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.MaterialFile;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.CourseRelationRepository;
import com.example.coursekb.mapper.ExportRecordRepository;
import com.example.coursekb.mapper.ExportTemplateRepository;
import com.example.coursekb.mapper.KnowledgeItemRepository;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.ExamKnowledgeStatVO;
import com.example.coursekb.vo.ExportRecordVO;
import com.example.coursekb.vo.ExportTemplateVO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {
    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CourseService courseService;
    private final ExportTemplateRepository exportTemplateRepository;
    private final ExportRecordRepository exportRecordRepository;
    private final CourseRelationRepository courseRelationRepository;
    private final ChapterRepository chapterRepository;
    private final MaterialRepository materialRepository;
    private final MaterialFileRepository materialFileRepository;
    private final TextChunkRepository textChunkRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final ExamQuestionService examQuestionService;
    private final JdbcTemplate jdbcTemplate;
    private final Path storageRoot;

    public ExportService(
            CourseService courseService,
            ExportTemplateRepository exportTemplateRepository,
            ExportRecordRepository exportRecordRepository,
            CourseRelationRepository courseRelationRepository,
            ChapterRepository chapterRepository,
            MaterialRepository materialRepository,
            MaterialFileRepository materialFileRepository,
            TextChunkRepository textChunkRepository,
            KnowledgeItemRepository knowledgeItemRepository,
            ExamQuestionService examQuestionService,
            JdbcTemplate jdbcTemplate,
            @Value("${ai4note.storage-root}") String storageRoot) {
        this.courseService = courseService;
        this.exportTemplateRepository = exportTemplateRepository;
        this.exportRecordRepository = exportRecordRepository;
        this.courseRelationRepository = courseRelationRepository;
        this.chapterRepository = chapterRepository;
        this.materialRepository = materialRepository;
        this.materialFileRepository = materialFileRepository;
        this.textChunkRepository = textChunkRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.examQuestionService = examQuestionService;
        this.jdbcTemplate = jdbcTemplate;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public List<ExportTemplateVO> listTemplates() {
        return exportTemplateRepository.findAll().stream()
                .sorted(Comparator.comparing(ExportTemplate::getId))
                .map(ExportTemplateVO::from)
                .collect(Collectors.toList());
    }

    public List<ExportRecordVO> listRecords(Long userId, Long courseId) {
        List<ExportRecord> records = courseId == null
                ? exportRecordRepository.findByUserIdOrderByExportTimeDescIdDesc(userId)
                : exportRecordRepository.findByUserIdAndCourseIdOrderByExportTimeDescIdDesc(userId, courseId);
        if (courseId != null) {
            courseService.getOwnedCourse(courseId, userId);
        }
        return records.stream().map(ExportRecordVO::from).collect(Collectors.toList());
    }

    @Transactional
    public ExportRecordVO exportCourse(ExportRequest request) {
        Course course = courseService.getOwnedCourse(request.getCourseId(), request.getUserId());
        ExportTemplate template = resolveTemplate(request.getTemplateId());
        String exportFormat = normalizeExportFormat(request.getExportFormat());
        String exportName = requireText(request.getExportName(), "导出名称不能为空");
        ExportScope scope = normalizeScope(request);

        ExportPayload payload = buildPayload(course, request.getUserId(), scope);
        Path outputPath = buildOutputPath(request.getUserId(), course.getId(), exportName);
        try {
            Files.createDirectories(outputPath.getParent());
            writeZip(outputPath, course, template, exportName, scope, payload);
        } catch (IOException exception) {
            throw new BusinessException("生成知识包失败");
        }

        ExportRecord record = new ExportRecord();
        record.setUserId(request.getUserId());
        record.setCourseId(course.getId());
        record.setTemplateId(template == null ? null : template.getId());
        record.setExportName(exportName);
        record.setExportFormat(exportFormat);
        record.setExportPath(storageRoot.relativize(outputPath).toString().replace('\\', '/'));
        record.setExportScope(scope.toJson());
        return ExportRecordVO.from(exportRecordRepository.save(record));
    }

    public Path resolveDownloadPath(Long exportId, Long userId) {
        ExportRecord record = exportRecordRepository.findByIdAndUserId(exportId, userId)
                .orElseThrow(() -> new BusinessException("导出记录不存在或无权访问"));
        courseService.getOwnedCourse(record.getCourseId(), userId);
        Path path = storageRoot.resolve(record.getExportPath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new BusinessException("导出文件不存在");
        }
        return path;
    }

    public ExportRecordVO getOwnedRecord(Long exportId, Long userId) {
        ExportRecord record = exportRecordRepository.findByIdAndUserId(exportId, userId)
                .orElseThrow(() -> new BusinessException("导出记录不存在或无权访问"));
        courseService.getOwnedCourse(record.getCourseId(), userId);
        return ExportRecordVO.from(record);
    }

    private ExportTemplate resolveTemplate(Long templateId) {
        if (templateId == null) {
            return exportTemplateRepository.findAll().stream()
                    .sorted(Comparator.comparing(ExportTemplate::getId))
                    .findFirst()
                    .orElse(null);
        }
        return exportTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("导出模板不存在"));
    }

    private String normalizeExportFormat(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "ZIP"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!"ZIP".equals(normalized)) {
            throw new BusinessException("当前仅支持 ZIP 导出");
        }
        return normalized;
    }

    private ExportScope normalizeScope(ExportRequest request) {
        ExportScope scope = new ExportScope();
        scope.chapterIds = normalizeLongSet(request.getChapterIds());
        scope.materialTypes = normalizeTextSet(request.getMaterialTypes());
        scope.onlyKeyMaterials = Boolean.TRUE.equals(request.getOnlyKeyMaterials());
        scope.includeExamStats = Boolean.TRUE.equals(request.getIncludeExamStats());
        scope.includePrerequisiteCourses = Boolean.TRUE.equals(request.getIncludePrerequisiteCourses());
        scope.includeRelatedCourses = Boolean.TRUE.equals(request.getIncludeRelatedCourses());
        scope.includeFollowUpCourses = Boolean.TRUE.equals(request.getIncludeFollowUpCourses());
        return scope;
    }

    private Set<Long> normalizeLongSet(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return values.stream().filter(value -> value != null).collect(Collectors.toSet());
    }

    private Set<String> normalizeTextSet(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return values.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private ExportPayload buildPayload(Course course, Long userId, ExportScope scope) {
        ExportPayload payload = new ExportPayload();
        payload.chapters = chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId());
        ensureChaptersBelongToCourse(scope.chapterIds, payload.chapters);
        payload.materials = materialRepository.findByCourseIdOrderByUploadTimeDesc(course.getId()).stream()
                .filter(material -> scope.chapterIds.isEmpty() || scope.chapterIds.contains(material.getChapterId()))
                .filter(material -> scope.materialTypes.isEmpty()
                        || scope.materialTypes.contains(material.getMaterialType()))
                .filter(material -> !scope.onlyKeyMaterials || Boolean.TRUE.equals(material.getKey()))
                .collect(Collectors.toList());
        payload.filesByMaterialId = loadFiles(payload.materials);
        payload.tagsByMaterialId = loadTags(payload.materials);
        payload.chunkCountsByMaterialId = loadChunkCounts(payload.materials);
        payload.knowledgeItems = knowledgeItemRepository.findByCourseIdOrderByImportanceLevelDescIdDesc(course.getId()).stream()
                .filter(item -> payload.materials.isEmpty()
                        || item.getMaterialId() == null
                        || payload.filesByMaterialId.containsKey(item.getMaterialId()))
                .filter(item -> scope.chapterIds.isEmpty() || scope.chapterIds.contains(item.getChapterId()))
                .collect(Collectors.toList());
        if (scope.includeExamStats) {
            payload.examStats = examQuestionService.listKnowledgeStats(course.getId(), userId, null, null, null);
        }
        payload.relatedCourses = loadRelatedCoursePayloads(course.getId(), userId, scope);
        return payload;
    }

    private List<RelatedCoursePayload> loadRelatedCoursePayloads(Long courseId, Long userId, ExportScope scope) {
        if (!scope.includePrerequisiteCourses && !scope.includeRelatedCourses && !scope.includeFollowUpCourses) {
            return Collections.emptyList();
        }
        List<RelatedCoursePayload> result = new ArrayList<>();
        for (CourseRelation relation : courseRelationRepository.findByCourseIdOrderBySortOrderAscIdAsc(courseId)) {
            if (!scope.includesRelationType(relation.getRelationType())) {
                continue;
            }
            Course relatedCourse = courseService.getOwnedCourse(relation.getRelatedCourseId(), userId);
            RelatedCoursePayload relatedPayload = new RelatedCoursePayload();
            relatedPayload.relation = relation;
            relatedPayload.course = relatedCourse;
            relatedPayload.chapters = chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(relatedCourse.getId());
            relatedPayload.materials = materialRepository.findByCourseIdOrderByUploadTimeDesc(relatedCourse.getId())
                    .stream()
                    .filter(material -> Boolean.TRUE.equals(material.getKey()))
                    .collect(Collectors.toList());
            relatedPayload.filesByMaterialId = loadFiles(relatedPayload.materials);
            relatedPayload.tagsByMaterialId = loadTags(relatedPayload.materials);
            relatedPayload.chunkCountsByMaterialId = loadChunkCounts(relatedPayload.materials);

            Set<Long> keyMaterialIds = relatedPayload.materials.stream()
                    .map(Material::getId)
                    .collect(Collectors.toSet());
            relatedPayload.knowledgeItems = knowledgeItemRepository
                    .findByCourseIdOrderByImportanceLevelDescIdDesc(relatedCourse.getId())
                    .stream()
                    .filter(item -> isRelatedKeyKnowledgeItem(item, keyMaterialIds))
                    .collect(Collectors.toList());
            result.add(relatedPayload);
            scope.addIncludedRelatedCourse(relation.getRelationType(), relatedCourse.getId());
        }
        return result;
    }

    private boolean isRelatedKeyKnowledgeItem(KnowledgeItem item, Set<Long> keyMaterialIds) {
        if (item.getMaterialId() != null && keyMaterialIds.contains(item.getMaterialId())) {
            return true;
        }
        if (item.getImportanceLevel() != null && item.getImportanceLevel() >= 4) {
            return true;
        }
        return "KEY_POINT".equals(item.getItemType());
    }

    private void ensureChaptersBelongToCourse(Set<Long> requestedChapterIds, List<Chapter> chapters) {
        if (requestedChapterIds.isEmpty()) {
            return;
        }
        Set<Long> existing = chapters.stream().map(Chapter::getId).collect(Collectors.toSet());
        if (!existing.containsAll(requestedChapterIds)) {
            throw new BusinessException("导出章节不属于当前课程");
        }
    }

    private Map<Long, MaterialFile> loadFiles(List<Material> materials) {
        Map<Long, MaterialFile> result = new LinkedHashMap<>();
        for (Material material : materials) {
            materialFileRepository.findByMaterialId(material.getId())
                    .ifPresent(file -> result.put(material.getId(), file));
        }
        return result;
    }

    private Map<Long, List<String>> loadTags(List<Material> materials) {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Material material : materials) {
            List<String> tags = jdbcTemplate.queryForList(
                    "SELECT t.tag_name FROM tag t JOIN material_tag mt ON mt.tag_id = t.id "
                            + "WHERE mt.material_id = ? ORDER BY t.tag_name",
                    String.class,
                    material.getId());
            result.put(material.getId(), tags);
        }
        return result;
    }

    private Map<Long, Long> loadChunkCounts(List<Material> materials) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Material material : materials) {
            result.put(material.getId(), textChunkRepository.countByMaterialId(material.getId()));
        }
        return result;
    }

    private Path buildOutputPath(Long userId, Long courseId, String exportName) {
        String fileName = slug(exportName) + "-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".zip";
        Path directory = storageRoot.resolve("exports")
                .resolve("user-" + userId)
                .resolve("course-" + courseId)
                .normalize();
        Path output = directory.resolve(fileName).normalize();
        if (!output.startsWith(directory) || !output.startsWith(storageRoot)) {
            throw new BusinessException("导出路径不合法");
        }
        return output;
    }

    private void writeZip(
            Path outputPath,
            Course course,
            ExportTemplate template,
            String exportName,
            ExportScope scope,
            ExportPayload payload) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(outputPath), StandardCharsets.UTF_8)) {
            addEntry(zip, "AGENTS.md", renderAgents(course, template));
            addEntry(zip, "README.md", renderReadme(course, exportName, scope, payload));
            addEntry(zip, "manifest.json", renderManifest(course, template, exportName, scope, payload));
            addEntry(zip, "index.md", renderIndex(course, payload));
            addEntry(zip, "context/course-profile.md", renderCourseProfile(course));
            addEntry(zip, "context/syllabus.md", renderSyllabus(payload.chapters));
            addEntry(zip, "context/related-courses.md", renderRelatedCourseIndex(payload.relatedCourses));
            addEntry(zip, "context/exam-scope.md", renderExamScope(payload.examStats));
            addEntry(zip, "materials/materials.md", renderMaterials(payload));
            addEntry(zip, "materials/related-course-materials.md", renderRelatedCourseMaterials(payload.relatedCourses));
            addEntry(zip, "summaries/key-points.md", renderKnowledgeByType(payload.knowledgeItems, "KEY_POINT", "Key Points"));
            addEntry(zip, "summaries/definitions.md", renderKnowledgeByType(payload.knowledgeItems, "DEFINITION", "Definitions"));
            addEntry(zip, "summaries/common-mistakes.md", renderKnowledgeByType(payload.knowledgeItems, "WARNING", "Common Mistakes"));
            addEntry(zip, "summaries/chapter-summaries.md", renderChapterSummaries(payload));
            addEntry(zip, "summaries/related-course-key-points.md", renderRelatedCourseKnowledge(payload.relatedCourses));
            addEntry(zip, "source/files.md", renderFiles(payload));
            addEntry(zip, "source/related-course-files.md", renderRelatedCourseFiles(payload.relatedCourses));
            addEntry(zip, "source/citations.md", renderCitations(payload));
            addEntry(zip, "prompts/course-tutor-prompt.md", "Use this package as course context. Cite material titles and pages when answering.\n");
            addEntry(zip, "prompts/exam-review-prompt.md", "Focus on high-frequency exam topics and weak knowledge areas in this package.\n");
            addEntry(zip, "prompts/lab-report-prompt.md", "Use source materials and chapter context to help draft lab reports accurately.\n");
        }
    }

    private void addEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String renderAgents(Course course, ExportTemplate template) {
        return "# Agent Instructions\n\n"
                + "This is an AI4Note course knowledge package for " + course.getCourseName() + ".\n\n"
                + "- Prefer facts from `materials/`, `summaries/`, and `source/`.\n"
                + "- Cite source material titles and page numbers when available.\n"
                + "- Do not invent course-specific facts that are not supported by this package.\n"
                + "- Target agent: " + (template == null ? "General" : template.getTargetAgent()) + "\n";
    }

    private String renderReadme(Course course, String exportName, ExportScope scope, ExportPayload payload) {
        return "# " + exportName + "\n\n"
                + "- Course: " + course.getCourseName() + "\n"
                + "- Code: " + valueOrDefault(course.getCourseCode(), "N/A") + "\n"
                + "- Semester: " + valueOrDefault(course.getSemester(), "N/A") + "\n"
                + "- Materials: " + payload.materials.size() + "\n"
                + "- Knowledge items: " + payload.knowledgeItems.size() + "\n"
                + "- Related courses: " + payload.relatedCourses.size() + "\n"
                + "- Scope: `" + scope.toJson() + "`\n\n"
                + "Start with `index.md` for the package map.\n";
    }

    private String renderManifest(
            Course course,
            ExportTemplate template,
            String exportName,
            ExportScope scope,
            ExportPayload payload) {
        return "{\n"
                + "  \"exportName\": \"" + json(exportName) + "\",\n"
                + "  \"courseId\": " + course.getId() + ",\n"
                + "  \"courseName\": \"" + json(course.getCourseName()) + "\",\n"
                + "  \"templateId\": " + (template == null ? "null" : template.getId()) + ",\n"
                + "  \"targetAgent\": \"" + json(template == null ? "General" : template.getTargetAgent()) + "\",\n"
                + "  \"materialCount\": " + payload.materials.size() + ",\n"
                + "  \"knowledgeItemCount\": " + payload.knowledgeItems.size() + ",\n"
                + "  \"relatedCourseCount\": " + payload.relatedCourses.size() + ",\n"
                + "  \"relatedCourses\": " + renderRelatedCoursesJson(payload.relatedCourses) + ",\n"
                + "  \"scope\": " + scope.toJson() + "\n"
                + "}\n";
    }

    private String renderIndex(Course course, ExportPayload payload) {
        StringBuilder builder = new StringBuilder("# Course Index\n\n");
        builder.append("## Course\n\n").append(course.getCourseName()).append("\n\n");
        builder.append("## Package Files\n\n");
        builder.append("- `context/course-profile.md`\n");
        builder.append("- `context/syllabus.md`\n");
        builder.append("- `materials/materials.md`\n");
        builder.append("- `materials/related-course-materials.md`\n");
        builder.append("- `summaries/key-points.md`\n");
        builder.append("- `summaries/related-course-key-points.md`\n");
        builder.append("- `source/citations.md`\n\n");
        builder.append("## Chapters\n\n");
        for (Chapter chapter : payload.chapters) {
            builder.append("- ").append(chapter.getChapterNo()).append(" ").append(chapter.getChapterTitle()).append("\n");
        }
        if (!payload.relatedCourses.isEmpty()) {
            builder.append("\n## Related Courses\n\n");
            for (RelatedCoursePayload related : payload.relatedCourses) {
                builder.append("- ").append(relationTypeLabel(related.relation.getRelationType()))
                        .append(": ").append(related.course.getCourseName()).append("\n");
            }
        }
        return builder.toString();
    }

    private String renderCourseProfile(Course course) {
        return "# Course Profile\n\n"
                + "- Name: " + course.getCourseName() + "\n"
                + "- Code: " + valueOrDefault(course.getCourseCode(), "N/A") + "\n"
                + "- Semester: " + valueOrDefault(course.getSemester(), "N/A") + "\n\n"
                + "## Description\n\n"
                + valueOrDefault(course.getDescription(), "No description.") + "\n";
    }

    private String renderSyllabus(List<Chapter> chapters) {
        StringBuilder builder = new StringBuilder("# Syllabus\n\n");
        for (Chapter chapter : chapters) {
            builder.append("- **").append(chapter.getChapterNo()).append("** ")
                    .append(chapter.getChapterTitle()).append("\n");
        }
        return builder.toString();
    }

    private String renderRelatedCourseIndex(List<RelatedCoursePayload> relatedCourses) {
        StringBuilder builder = new StringBuilder("# Related Courses\n\n");
        if (relatedCourses.isEmpty()) {
            return builder.append("No related course content is included.\n").toString();
        }
        for (RelatedCoursePayload related : relatedCourses) {
            builder.append("## ").append(relationTypeLabel(related.relation.getRelationType()))
                    .append(": ").append(related.course.getCourseName()).append("\n\n");
            builder.append("- Code: ").append(valueOrDefault(related.course.getCourseCode(), "N/A")).append("\n");
            builder.append("- Semester: ").append(valueOrDefault(related.course.getSemester(), "N/A")).append("\n");
            builder.append("- Reason: ").append(valueOrDefault(related.relation.getReason(), "N/A")).append("\n");
            builder.append("- Key materials: ").append(related.materials.size()).append("\n");
            builder.append("- Key knowledge items: ").append(related.knowledgeItems.size()).append("\n\n");
            builder.append(valueOrDefault(related.course.getDescription(), "No description.")).append("\n\n");
        }
        return builder.toString();
    }

    private String renderRelatedCourseMaterials(List<RelatedCoursePayload> relatedCourses) {
        StringBuilder builder = new StringBuilder("# Related Course Materials\n\n");
        if (relatedCourses.isEmpty()) {
            return builder.append("No related course materials are included.\n").toString();
        }
        for (RelatedCoursePayload related : relatedCourses) {
            builder.append("## ").append(relationTypeLabel(related.relation.getRelationType()))
                    .append(": ").append(related.course.getCourseName()).append("\n\n");
            if (related.materials.isEmpty()) {
                builder.append("No key materials are available for this related course.\n\n");
                continue;
            }
            for (Material material : related.materials) {
                MaterialFile file = related.filesByMaterialId.get(material.getId());
                builder.append("### ").append(material.getTitle()).append("\n\n");
                builder.append("- Type: ").append(material.getMaterialType()).append("\n");
                builder.append("- Year: ").append(valueOrDefault(material.getYear(), "N/A")).append("\n");
                builder.append("- File: ").append(file == null ? "N/A" : file.getOriginalName()).append("\n");
                builder.append("- Parsed chunks: ")
                        .append(related.chunkCountsByMaterialId.getOrDefault(material.getId(), 0L)).append("\n");
                builder.append("- Tags: ")
                        .append(String.join(", ", related.tagsByMaterialId.getOrDefault(
                                material.getId(), Collections.emptyList()))).append("\n\n");
                builder.append(valueOrDefault(material.getSummary(), "No summary.")).append("\n\n");
            }
        }
        return builder.toString();
    }

    private String renderRelatedCourseKnowledge(List<RelatedCoursePayload> relatedCourses) {
        StringBuilder builder = new StringBuilder("# Related Course Key Points\n\n");
        if (relatedCourses.isEmpty()) {
            return builder.append("No related course knowledge is included.\n").toString();
        }
        for (RelatedCoursePayload related : relatedCourses) {
            builder.append("## ").append(relationTypeLabel(related.relation.getRelationType()))
                    .append(": ").append(related.course.getCourseName()).append("\n\n");
            if (related.knowledgeItems.isEmpty()) {
                builder.append("No key knowledge items are available for this related course.\n\n");
                continue;
            }
            for (KnowledgeItem item : related.knowledgeItems) {
                builder.append("### ").append(item.getTitle()).append("\n\n")
                        .append("- Type: ").append(item.getItemType()).append("\n")
                        .append("- Importance: ").append(item.getImportanceLevel()).append("\n")
                        .append("- Source page: ").append(valueOrDefault(item.getSourcePage(), "N/A")).append("\n\n")
                        .append(item.getContent()).append("\n\n");
            }
        }
        return builder.toString();
    }

    private String renderRelatedCourseFiles(List<RelatedCoursePayload> relatedCourses) {
        StringBuilder builder = new StringBuilder("# Related Course Source Files\n\n");
        if (relatedCourses.isEmpty()) {
            return builder.append("No related course source files are included.\n").toString();
        }
        for (RelatedCoursePayload related : relatedCourses) {
            builder.append("## ").append(relationTypeLabel(related.relation.getRelationType()))
                    .append(": ").append(related.course.getCourseName()).append("\n\n");
            for (Material material : related.materials) {
                MaterialFile file = related.filesByMaterialId.get(material.getId());
                builder.append("- ").append(material.getTitle()).append(": ")
                        .append(file == null ? "N/A" : file.getOriginalName())
                        .append(" (").append(material.getMaterialType()).append(")\n");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String renderExamScope(List<ExamKnowledgeStatVO> stats) {
        StringBuilder builder = new StringBuilder("# Exam Scope\n\n");
        if (stats.isEmpty()) {
            return builder.append("No exam knowledge statistics are included.\n").toString();
        }
        for (ExamKnowledgeStatVO stat : stats) {
            builder.append("- **").append(stat.getKnowledgeTitle()).append("**: ")
                    .append(stat.getQuestionCount()).append(" questions, ")
                    .append(stat.getTotalScore()).append(" total score, latest year ")
                    .append(valueOrDefault(stat.getLatestExamYear(), "unknown")).append("\n");
        }
        return builder.toString();
    }

    private String renderMaterials(ExportPayload payload) {
        StringBuilder builder = new StringBuilder("# Materials\n\n");
        for (Material material : payload.materials) {
            MaterialFile file = payload.filesByMaterialId.get(material.getId());
            builder.append("## ").append(material.getTitle()).append("\n\n");
            builder.append("- Type: ").append(material.getMaterialType()).append("\n");
            builder.append("- Year: ").append(valueOrDefault(material.getYear(), "N/A")).append("\n");
            builder.append("- Key: ").append(Boolean.TRUE.equals(material.getKey()) ? "yes" : "no").append("\n");
            builder.append("- File: ").append(file == null ? "N/A" : file.getOriginalName()).append("\n");
            builder.append("- Parsed chunks: ").append(payload.chunkCountsByMaterialId.getOrDefault(material.getId(), 0L)).append("\n");
            builder.append("- Tags: ").append(String.join(", ", payload.tagsByMaterialId.getOrDefault(material.getId(), Collections.emptyList()))).append("\n\n");
            builder.append(valueOrDefault(material.getSummary(), "No summary.")).append("\n\n");
        }
        return builder.toString();
    }

    private String renderKnowledgeByType(List<KnowledgeItem> items, String type, String title) {
        StringBuilder builder = new StringBuilder("# ").append(title).append("\n\n");
        for (KnowledgeItem item : items) {
            if (!type.equals(item.getItemType())) {
                continue;
            }
            builder.append("## ").append(item.getTitle()).append("\n\n")
                    .append("- Importance: ").append(item.getImportanceLevel()).append("\n")
                    .append("- Source page: ").append(valueOrDefault(item.getSourcePage(), "N/A")).append("\n\n")
                    .append(item.getContent()).append("\n\n");
        }
        return builder.toString();
    }

    private String renderChapterSummaries(ExportPayload payload) {
        Map<Long, List<KnowledgeItem>> byChapter = payload.knowledgeItems.stream()
                .filter(item -> item.getChapterId() != null)
                .collect(Collectors.groupingBy(KnowledgeItem::getChapterId, LinkedHashMap::new, Collectors.toList()));
        StringBuilder builder = new StringBuilder("# Chapter Summaries\n\n");
        for (Chapter chapter : payload.chapters) {
            builder.append("## ").append(chapter.getChapterNo()).append(" ").append(chapter.getChapterTitle()).append("\n\n");
            List<KnowledgeItem> items = byChapter.getOrDefault(chapter.getId(), Collections.emptyList());
            if (items.isEmpty()) {
                builder.append("No knowledge items linked to this chapter.\n\n");
                continue;
            }
            for (KnowledgeItem item : items) {
                builder.append("- **").append(item.getTitle()).append("**: ")
                        .append(firstLine(item.getContent())).append("\n");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String renderFiles(ExportPayload payload) {
        StringBuilder builder = new StringBuilder("# Source Files\n\n");
        for (Material material : payload.materials) {
            MaterialFile file = payload.filesByMaterialId.get(material.getId());
            builder.append("- ").append(material.getTitle()).append(": ")
                    .append(file == null ? "N/A" : file.getOriginalName())
                    .append(" (").append(material.getMaterialType()).append(")\n");
        }
        return builder.toString();
    }

    private String renderCitations(ExportPayload payload) {
        StringBuilder builder = new StringBuilder("# Citations\n\n");
        for (KnowledgeItem item : payload.knowledgeItems) {
            builder.append("- ").append(item.getTitle()).append(": ");
            if (item.getMaterialId() == null) {
                builder.append("manual knowledge item");
            } else {
                Material material = payload.materials.stream()
                        .filter(value -> item.getMaterialId().equals(value.getId()))
                        .findFirst()
                        .orElse(null);
                builder.append(material == null ? "unknown material" : material.getTitle());
            }
            if (item.getSourcePage() != null) {
                builder.append(", page ").append(item.getSourcePage());
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String firstLine(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "...";
    }

    private String renderRelatedCoursesJson(List<RelatedCoursePayload> relatedCourses) {
        return relatedCourses.stream()
                .map(related -> "{"
                        + "\"courseId\":" + related.course.getId() + ","
                        + "\"courseName\":\"" + json(related.course.getCourseName()) + "\","
                        + "\"relationType\":\"" + json(related.relation.getRelationType()) + "\","
                        + "\"keyMaterialCount\":" + related.materials.size() + ","
                        + "\"keyKnowledgeItemCount\":" + related.knowledgeItems.size()
                        + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String relationTypeLabel(String relationType) {
        if ("PREREQUISITE".equals(relationType)) {
            return "Prerequisite";
        }
        if ("RELATED".equals(relationType)) {
            return "Related";
        }
        if ("FOLLOW_UP".equals(relationType)) {
            return "Follow-up";
        }
        return valueOrDefault(relationType, "Related");
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String slug(String value) {
        String normalized = value == null ? "export" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "export" : normalized;
    }

    private String valueOrDefault(Object value, String fallback) {
        return value == null || value.toString().trim().isEmpty() ? fallback : value.toString();
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static class ExportScope {
        private Set<Long> chapterIds = new HashSet<>();
        private Set<String> materialTypes = new HashSet<>();
        private boolean onlyKeyMaterials;
        private boolean includeExamStats;
        private boolean includePrerequisiteCourses;
        private boolean includeRelatedCourses;
        private boolean includeFollowUpCourses;
        private Set<Long> includedPrerequisiteCourseIds = new HashSet<>();
        private Set<Long> includedRelatedCourseIds = new HashSet<>();
        private Set<Long> includedFollowUpCourseIds = new HashSet<>();

        private boolean includesRelationType(String relationType) {
            if ("PREREQUISITE".equals(relationType)) {
                return includePrerequisiteCourses;
            }
            if ("RELATED".equals(relationType)) {
                return includeRelatedCourses;
            }
            if ("FOLLOW_UP".equals(relationType)) {
                return includeFollowUpCourses;
            }
            return false;
        }

        private void addIncludedRelatedCourse(String relationType, Long courseId) {
            if ("PREREQUISITE".equals(relationType)) {
                includedPrerequisiteCourseIds.add(courseId);
            } else if ("RELATED".equals(relationType)) {
                includedRelatedCourseIds.add(courseId);
            } else if ("FOLLOW_UP".equals(relationType)) {
                includedFollowUpCourseIds.add(courseId);
            }
        }

        private String toJson() {
            return "{"
                    + "\"chapterIds\":[" + chapterIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(",")) + "],"
                    + "\"materialTypes\":[" + materialTypes.stream().sorted().map(value -> "\"" + value + "\"").collect(Collectors.joining(",")) + "],"
                    + "\"onlyKeyMaterials\":" + onlyKeyMaterials + ","
                    + "\"includeExamStats\":" + includeExamStats + ","
                    + "\"includePrerequisiteCourses\":" + includePrerequisiteCourses + ","
                    + "\"includeRelatedCourses\":" + includeRelatedCourses + ","
                    + "\"includeFollowUpCourses\":" + includeFollowUpCourses + ","
                    + "\"includedPrerequisiteCourseIds\":[" + joinIds(includedPrerequisiteCourseIds) + "],"
                    + "\"includedRelatedCourseIds\":[" + joinIds(includedRelatedCourseIds) + "],"
                    + "\"includedFollowUpCourseIds\":[" + joinIds(includedFollowUpCourseIds) + "]"
                    + "}";
        }

        private String joinIds(Set<Long> ids) {
            return ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        }
    }

    private static class ExportPayload {
        private List<Chapter> chapters = new ArrayList<>();
        private List<Material> materials = new ArrayList<>();
        private Map<Long, MaterialFile> filesByMaterialId = new LinkedHashMap<>();
        private Map<Long, List<String>> tagsByMaterialId = new LinkedHashMap<>();
        private Map<Long, Long> chunkCountsByMaterialId = new LinkedHashMap<>();
        private List<KnowledgeItem> knowledgeItems = new ArrayList<>();
        private List<ExamKnowledgeStatVO> examStats = new ArrayList<>();
        private List<RelatedCoursePayload> relatedCourses = new ArrayList<>();
    }

    private static class RelatedCoursePayload {
        private CourseRelation relation;
        private Course course;
        private List<Chapter> chapters = new ArrayList<>();
        private List<Material> materials = new ArrayList<>();
        private Map<Long, MaterialFile> filesByMaterialId = new LinkedHashMap<>();
        private Map<Long, List<String>> tagsByMaterialId = new LinkedHashMap<>();
        private Map<Long, Long> chunkCountsByMaterialId = new LinkedHashMap<>();
        private List<KnowledgeItem> knowledgeItems = new ArrayList<>();
    }
}

package com.example.coursekb.controller;

import com.example.coursekb.dto.MaterialUpdateRequest;
import com.example.coursekb.dto.MaterialSummaryGenerateRequest;
import com.example.coursekb.service.ContentDeletionService;
import com.example.coursekb.service.MaterialService;
import com.example.coursekb.service.MaterialSimilarityService;
import com.example.coursekb.service.MaterialSummaryService;
import com.example.coursekb.service.PdfParseService;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.entity.MaterialFile;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.vo.MaterialVO;
import com.example.coursekb.vo.MaterialSimilarityVO;
import com.example.coursekb.vo.PdfParseResultVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialSimilarityService materialSimilarityService;
    private final MaterialSummaryService materialSummaryService;
    private final PdfParseService pdfParseService;
    private final ContentDeletionService contentDeletionService;

    public MaterialController(
            MaterialService materialService,
            MaterialSimilarityService materialSimilarityService,
            MaterialSummaryService materialSummaryService,
            PdfParseService pdfParseService,
            ContentDeletionService contentDeletionService) {
        this.materialService = materialService;
        this.materialSimilarityService = materialSimilarityService;
        this.materialSummaryService = materialSummaryService;
        this.pdfParseService = pdfParseService;
        this.contentDeletionService = contentDeletionService;
    }

    @GetMapping("/courses/{courseId}/materials")
    public List<MaterialVO> list(
            @PathVariable Long courseId,
            @RequestParam Long userId) {
        return materialService.list(courseId, userId);
    }

    @GetMapping("/materials/{materialId}")
    public MaterialVO detail(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        return materialService.detail(materialId, userId);
    }

    @GetMapping("/materials/{materialId}/preview")
    public ResponseEntity<String> preview(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        MaterialFile file = materialService.getOwnedMaterialFile(materialId, userId);
        String fileType = file.getFileType() == null ? "" : file.getFileType().toLowerCase(Locale.ROOT);
        if (!"md".equals(fileType) && !"txt".equals(fileType) && !"doc".equals(fileType) && !"docx".equals(fileType)) {
            throw new BusinessException("Current material is not a text preview file");
        }
        try {
            String content = readPreviewContent(materialService.resolveFilePath(materialId, userId), fileType);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                    .body(content);
        } catch (IOException exception) {
            throw new BusinessException("Failed to read material preview");
        }
    }

    @GetMapping("/materials/{materialId}/file")
    public ResponseEntity<Resource> file(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "inline") String disposition) {
        MaterialFile file = materialService.getOwnedMaterialFile(materialId, userId);
        Resource resource = new FileSystemResource(materialService.resolveFilePath(materialId, userId).toFile());
        String fileType = file.getFileType() == null ? "" : file.getFileType().toLowerCase(Locale.ROOT);
        MediaType contentType;
        if ("pdf".equals(fileType)) {
            contentType = MediaType.APPLICATION_PDF;
        } else if ("md".equals(fileType)) {
            contentType = MediaType.parseMediaType("text/markdown; charset=UTF-8");
        } else if ("txt".equals(fileType)) {
            contentType = MediaType.TEXT_PLAIN;
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition contentDisposition = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename(file.getOriginalName(), StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(file.getOriginalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    private String readPreviewContent(Path path, String fileType) throws IOException {
        if ("md".equals(fileType) || "txt".equals(fileType)) {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        if ("docx".equals(fileType)) {
            try (InputStream inputStream = Files.newInputStream(path);
                    XWPFDocument document = new XWPFDocument(inputStream);
                    XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }
        if ("doc".equals(fileType)) {
            try (InputStream inputStream = Files.newInputStream(path);
                    HWPFDocument document = new HWPFDocument(inputStream);
                    WordExtractor extractor = new WordExtractor(document)) {
                return extractor.getText();
            }
        }
        throw new BusinessException("Unsupported material preview type");
    }

    @GetMapping("/materials/{materialId}/similar")
    public List<MaterialSimilarityVO> similarMaterials(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        return materialSimilarityService.detect(materialId, userId);
    }

    @PostMapping("/materials")
    public MaterialVO upload(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam String title,
            @RequestParam String materialType,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "false") Boolean isKey,
            @RequestParam(required = false) String summary,
            @RequestParam MultipartFile file) {
        return materialService.upload(
                userId,
                courseId,
                chapterId,
                title,
                materialType,
                year,
                isKey,
                summary,
                file);
    }

    @PostMapping("/materials/batch")
    public List<MaterialVO> uploadBatch(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam List<MultipartFile> files,
            @RequestParam List<String> titles,
            @RequestParam List<String> materialTypes,
            @RequestParam(required = false) List<String> chapterIds,
            @RequestParam(required = false) List<String> years,
            @RequestParam(required = false) List<String> isKeys,
            @RequestParam(required = false) List<String> summaries) {
        int total = files == null ? 0 : files.size();
        if (total == 0) {
            throw new BusinessException("请选择需要上传的资料文件");
        }
        requireSize(titles, total, "资料标题数量与文件数量不一致");
        requireSize(materialTypes, total, "资料类型数量与文件数量不一致");

        List<MaterialVO> uploaded = new ArrayList<>();
        for (int index = 0; index < total; index++) {
            uploaded.add(materialService.upload(
                    userId,
                    courseId,
                    parseLong(optionalValue(chapterIds, index), "章节参数不正确"),
                    requiredValue(titles, index, "资料标题数量与文件数量不一致"),
                    requiredValue(materialTypes, index, "资料类型数量与文件数量不一致"),
                    parseInteger(optionalValue(years, index), "年份参数不正确"),
                    parseBoolean(optionalValue(isKeys, index)),
                    optionalValue(summaries, index),
                    files.get(index)));
        }
        return uploaded;
    }

    @PutMapping("/materials/{materialId}")
    public MaterialVO update(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @javax.validation.Valid @RequestBody MaterialUpdateRequest request) {
        return materialService.update(materialId, userId, request);
    }

    @DeleteMapping("/materials/{materialId}")
    public void delete(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        contentDeletionService.deleteMaterial(materialId, userId);
    }

    @PostMapping("/materials/{materialId}/parse")
    public PdfParseResultVO parse(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        return pdfParseService.parse(materialId, userId);
    }

    @PostMapping("/materials/{materialId}/summary/ai-generate")
    public MaterialVO generateSummary(
            @PathVariable Long materialId,
            @RequestParam Long userId,
            @RequestBody(required = false) MaterialSummaryGenerateRequest request) {
        return materialSummaryService.generate(
                materialId,
                userId,
                request == null ? new MaterialSummaryGenerateRequest() : request);
    }

    @GetMapping("/materials/{materialId}/text-chunks")
    public List<TextChunk> listTextChunks(
            @PathVariable Long materialId,
            @RequestParam Long userId) {
        return pdfParseService.listChunks(materialId, userId);
    }

    private void requireSize(List<?> values, int expected, String message) {
        if (values == null || values.size() != expected) {
            throw new BusinessException(message);
        }
    }

    private String requiredValue(List<String> values, int index, String message) {
        if (values == null || index >= values.size()) {
            throw new BusinessException(message);
        }
        return values.get(index);
    }

    private String optionalValue(List<String> values, int index) {
        if (values == null || index >= values.size()) {
            return null;
        }
        String value = values.get(index);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private Long parseLong(String value, String message) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(message);
        }
    }

    private Integer parseInteger(String value, String message) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(message);
        }
    }

    private Boolean parseBoolean(String value) {
        return value != null && Boolean.parseBoolean(value);
    }
}

package com.example.coursekb.controller;

import com.example.coursekb.dto.ExportRequest;
import com.example.coursekb.service.ExportService;
import com.example.coursekb.vo.ExportRecordVO;
import com.example.coursekb.vo.ExportTemplateVO;
import java.nio.file.Path;
import java.util.List;
import javax.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExportController {
    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/export-templates")
    public List<ExportTemplateVO> listTemplates() {
        return exportService.listTemplates();
    }

    @GetMapping("/exports")
    public List<ExportRecordVO> listRecords(
            @RequestParam Long userId,
            @RequestParam(required = false) Long courseId) {
        return exportService.listRecords(userId, courseId);
    }

    @PostMapping("/exports")
    public ExportRecordVO exportCourse(@Valid @RequestBody ExportRequest request) {
        return exportService.exportCourse(request);
    }

    @GetMapping("/exports/{exportId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long exportId,
            @RequestParam Long userId) {
        ExportRecordVO record = exportService.getOwnedRecord(exportId, userId);
        Path path = exportService.resolveDownloadPath(exportId, userId);
        Resource resource = new FileSystemResource(path.toFile());
        String filename = record.getExportName() + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(resource);
    }
}

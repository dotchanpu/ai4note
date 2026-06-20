package com.example.coursekb.controller;

import com.example.coursekb.dto.KnowledgeGapReportRequest;
import com.example.coursekb.service.KnowledgeGapService;
import com.example.coursekb.vo.KnowledgeGapItemVO;
import com.example.coursekb.vo.KnowledgeGapReportVO;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class KnowledgeGapController {
    private final KnowledgeGapService knowledgeGapService;

    public KnowledgeGapController(KnowledgeGapService knowledgeGapService) {
        this.knowledgeGapService = knowledgeGapService;
    }

    @GetMapping("/courses/{courseId}/knowledge-gap-reports")
    public List<KnowledgeGapReportVO> listReports(
            @PathVariable Long courseId,
            @RequestParam Long userId) {
        return knowledgeGapService.listReports(courseId, userId);
    }

    @PostMapping("/courses/{courseId}/knowledge-gap-reports")
    public KnowledgeGapReportVO generateReport(
            @PathVariable Long courseId,
            @Valid @RequestBody KnowledgeGapReportRequest request) {
        return knowledgeGapService.generate(courseId, request);
    }

    @GetMapping("/courses/{courseId}/prerequisite-gap-hints")
    public List<KnowledgeGapItemVO> listPrerequisiteHints(
            @PathVariable Long courseId,
            @RequestParam Long userId) {
        return knowledgeGapService.listPrerequisiteHints(courseId, userId);
    }

    @GetMapping("/knowledge-gap-reports/{reportId}")
    public KnowledgeGapReportVO getReport(
            @PathVariable Long reportId,
            @RequestParam Long userId) {
        return knowledgeGapService.getReport(reportId, userId);
    }

    @GetMapping("/knowledge-gap-reports/{reportId}/items")
    public List<KnowledgeGapItemVO> listItems(
            @PathVariable Long reportId,
            @RequestParam Long userId) {
        return knowledgeGapService.listItems(reportId, userId);
    }
}

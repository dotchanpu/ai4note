package com.example.coursekb.controller;

import com.example.coursekb.service.SearchService;
import com.example.coursekb.vo.MaterialSearchResultVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<MaterialSearchResultVO> search(
            @RequestParam Long userId,
            @RequestParam Long courseId,
            @RequestParam String keyword,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String materialType,
            @RequestParam(defaultValue = "false") Boolean isKey) {
        return searchService.search(
                userId,
                courseId,
                keyword,
                chapterId,
                materialType,
                isKey);
    }
}

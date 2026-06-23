package com.example.coursekb.vo;

import java.util.Collections;
import java.util.List;

public class ExamQuestionPageVO {
    private List<ExamQuestionVO> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public static ExamQuestionPageVO of(
            List<ExamQuestionVO> items, long total, int page, int size, int totalPages) {
        ExamQuestionPageVO result = new ExamQuestionPageVO();
        result.items = items == null ? Collections.emptyList() : items;
        result.total = total;
        result.page = page;
        result.size = size;
        result.totalPages = totalPages;
        return result;
    }

    public List<ExamQuestionVO> getItems() {
        return items;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }
}

package com.example.coursekb.vo;

public class PdfParseResultVO {
    private final Long materialId;
    private final int pageCount;
    private final int chunkCount;
    private final int characterCount;

    public PdfParseResultVO(Long materialId, int pageCount, int chunkCount, int characterCount) {
        this.materialId = materialId;
        this.pageCount = pageCount;
        this.chunkCount = chunkCount;
        this.characterCount = characterCount;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public int getCharacterCount() {
        return characterCount;
    }
}

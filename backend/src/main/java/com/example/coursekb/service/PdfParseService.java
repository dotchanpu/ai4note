package com.example.coursekb.service;

import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.MaterialFile;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.PdfParseResultVO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PdfParseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfParseService.class);

    private final MaterialService materialService;
    private final MaterialFileRepository materialFileRepository;
    private final TextChunkRepository textChunkRepository;
    private final TextChunker textChunker;

    public PdfParseService(
            MaterialService materialService,
            MaterialFileRepository materialFileRepository,
            TextChunkRepository textChunkRepository,
            TextChunker textChunker) {
        this.materialService = materialService;
        this.materialFileRepository = materialFileRepository;
        this.textChunkRepository = textChunkRepository;
        this.textChunker = textChunker;
    }

    @Transactional
    public PdfParseResultVO parse(Long materialId, Long userId) {
        Material material = materialService.getOwnedMaterial(materialId, userId);
        MaterialFile materialFile = materialFileRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException("资料文件不存在"));
        if (!"pdf".equals(materialFile.getFileType().toLowerCase(Locale.ROOT))) {
            throw new BusinessException("当前仅支持解析 PDF 文件");
        }

        Path filePath = materialService.resolveFilePath(materialId, userId);
        List<TextChunk> chunks = new ArrayList<>();
        int pageCount;
        int characterCount = 0;

        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String content = normalize(stripper.getText(document));
                if (content.isEmpty()) {
                    continue;
                }

                for (String chunkContent : textChunker.split(content)) {
                    TextChunk chunk = new TextChunk();
                    chunk.setMaterialId(material.getId());
                    chunk.setChunkIndex(chunks.size());
                    chunk.setPageNo(page);
                    chunk.setContent(chunkContent);
                    chunk.setWordCount(countContentUnits(chunkContent));
                    chunks.add(chunk);
                }
                characterCount += content.length();
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to parse PDF material {}", materialId, exception);
            throw new BusinessException("PDF 解析失败，请确认文件未损坏或未加密");
        }

        if (chunks.isEmpty()) {
            throw new BusinessException("PDF 中未提取到可用文字，可能是扫描版文件");
        }

        textChunkRepository.deleteByMaterialId(materialId);
        textChunkRepository.flush();
        textChunkRepository.saveAll(chunks);
        return new PdfParseResultVO(materialId, pageCount, chunks.size(), characterCount);
    }

    public List<TextChunk> listChunks(Long materialId, Long userId) {
        materialService.getOwnedMaterial(materialId, userId);
        return textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(materialId);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u0000", "")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim();
    }

    private int countContentUnits(String content) {
        return content.replaceAll("\\s+", "").length();
    }
}

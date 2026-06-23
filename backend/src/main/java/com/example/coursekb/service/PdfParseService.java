package com.example.coursekb.service;

import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.MaterialFile;
import com.example.coursekb.entity.TextChunk;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.PdfParseResultVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PdfParseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfParseService.class);
    private static final Set<String> SUPPORTED_PARSE_TYPES = new HashSet<>(
            Arrays.asList("pdf", "doc", "docx", "md", "txt"));

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
                .orElseThrow(() -> new BusinessException("Material file does not exist"));
        String fileType = materialFile.getFileType() == null
                ? ""
                : materialFile.getFileType().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_PARSE_TYPES.contains(fileType)) {
            throw new BusinessException("Only PDF, Word, Markdown and TXT files can be parsed");
        }

        Path filePath = materialService.resolveFilePath(materialId, userId);
        ParsePayload payload;
        try {
            payload = "pdf".equals(fileType)
                    ? parsePdf(material, filePath)
                    : parseTextMaterial(material, filePath, fileType);
        } catch (IOException exception) {
            LOGGER.error("Failed to parse material {} as {}", materialId, fileType, exception);
            throw new BusinessException("Material parse failed. Please confirm the file is readable");
        }

        if (payload.chunks.isEmpty()) {
            throw new BusinessException("No readable text was extracted from this material");
        }

        textChunkRepository.deleteByMaterialId(materialId);
        textChunkRepository.flush();
        textChunkRepository.saveAll(payload.chunks);
        return new PdfParseResultVO(materialId, payload.pageCount, payload.chunks.size(), payload.characterCount);
    }

    public List<TextChunk> listChunks(Long materialId, Long userId) {
        materialService.getOwnedMaterial(materialId, userId);
        return textChunkRepository.findByMaterialIdOrderByChunkIndexAsc(materialId);
    }

    private ParsePayload parsePdf(Material material, Path filePath) throws IOException {
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

                chunks.addAll(createChunks(material, content, page, chunks.size()));
                characterCount += content.length();
            }
        }
        return new ParsePayload(pageCount, chunks, characterCount);
    }

    private ParsePayload parseTextMaterial(Material material, Path filePath, String fileType) throws IOException {
        String content = normalize(readTextContent(filePath, fileType));
        List<TextChunk> chunks = createChunks(material, content, 1, 0);
        return new ParsePayload(content.isEmpty() ? 0 : 1, chunks, content.length());
    }

    private String readTextContent(Path filePath, String fileType) throws IOException {
        if ("md".equals(fileType) || "txt".equals(fileType)) {
            return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        }
        if ("docx".equals(fileType)) {
            try (InputStream inputStream = Files.newInputStream(filePath);
                    XWPFDocument document = new XWPFDocument(inputStream);
                    XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }
        if ("doc".equals(fileType)) {
            try (InputStream inputStream = Files.newInputStream(filePath);
                    HWPFDocument document = new HWPFDocument(inputStream);
                    WordExtractor extractor = new WordExtractor(document)) {
                return extractor.getText();
            }
        }
        throw new BusinessException("Unsupported material parse type");
    }

    private List<TextChunk> createChunks(Material material, String content, Integer pageNo, int startIndex) {
        List<TextChunk> chunks = new ArrayList<>();
        if (content.isEmpty()) {
            return chunks;
        }
        int chunkIndex = startIndex;
        for (String chunkContent : textChunker.split(content)) {
            TextChunk chunk = new TextChunk();
            chunk.setMaterialId(material.getId());
            chunk.setChunkIndex(chunkIndex++);
            chunk.setPageNo(pageNo);
            chunk.setContent(chunkContent);
            chunk.setWordCount(countContentUnits(chunkContent));
            chunks.add(chunk);
        }
        return chunks;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace("\u0000", "")
                .replace('\uF06E', '\u2028')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f ]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim();

        String[] paragraphs = cleaned.split("\\n\\s*\\n+");
        StringBuilder normalized = new StringBuilder();
        for (String paragraph : paragraphs) {
            String merged = mergeWrappedLines(paragraph);
            if (merged.isEmpty()) {
                continue;
            }
            if (normalized.length() > 0) {
                normalized.append("\n\n");
            }
            normalized.append(merged);
        }
        return normalized.toString().replace('\u2028', '\n');
    }

    private String mergeWrappedLines(String paragraph) {
        String[] lines = paragraph.split("\\n+");
        StringBuilder result = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (result.length() == 0) {
                result.append(line);
                continue;
            }

            if (isListItem(line)) {
                result.append('\n').append(line);
            } else if (endsWithLatinHyphen(result) && startsWithLatinLetter(line)) {
                result.setLength(result.length() - 1);
                result.append(line);
            } else if (needsSpace(result.charAt(result.length() - 1), line.charAt(0))) {
                result.append(' ').append(line);
            } else {
                result.append(line);
            }
        }
        return result.toString();
    }

    private boolean isListItem(String line) {
        return line.matches("^(?:[-*\\u2022\\u25AA]|\\d+[.)\\u3001]|[\\uff08(]?[\\u4e00-\\u9fa5]+[\\uff09)\\u3001.])\\s*.*");
    }

    private boolean endsWithLatinHyphen(StringBuilder value) {
        int length = value.length();
        return length >= 2
                && value.charAt(length - 1) == '-'
                && isLatinLetter(value.charAt(length - 2));
    }

    private boolean startsWithLatinLetter(String value) {
        return !value.isEmpty() && isLatinLetter(value.charAt(0));
    }

    private boolean needsSpace(char previous, char next) {
        return (isLatinLetter(previous) || Character.isDigit(previous))
                && (isLatinLetter(next) || Character.isDigit(next));
    }

    private boolean isLatinLetter(char value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
    }

    private int countContentUnits(String content) {
        return content.replaceAll("\\s+", "").length();
    }

    private static class ParsePayload {
        private final int pageCount;
        private final List<TextChunk> chunks;
        private final int characterCount;

        private ParsePayload(int pageCount, List<TextChunk> chunks, int characterCount) {
            this.pageCount = pageCount;
            this.chunks = chunks;
            this.characterCount = characterCount;
        }
    }
}

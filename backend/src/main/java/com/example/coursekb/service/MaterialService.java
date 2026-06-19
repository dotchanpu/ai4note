package com.example.coursekb.service;

import com.example.coursekb.dto.MaterialUpdateRequest;
import com.example.coursekb.entity.Material;
import com.example.coursekb.entity.MaterialFile;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ChapterRepository;
import com.example.coursekb.mapper.MaterialFileRepository;
import com.example.coursekb.mapper.MaterialRepository;
import com.example.coursekb.mapper.TextChunkRepository;
import com.example.coursekb.vo.MaterialVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MaterialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialService.class);
    private static final Set<String> SUPPORTED_EXTENSIONS =
            new HashSet<>(Arrays.asList("pdf", "doc", "docx", "md", "txt"));

    private final MaterialRepository materialRepository;
    private final MaterialFileRepository materialFileRepository;
    private final ChapterRepository chapterRepository;
    private final TextChunkRepository textChunkRepository;
    private final CourseService courseService;
    private final Path storageRoot;

    public MaterialService(
            MaterialRepository materialRepository,
            MaterialFileRepository materialFileRepository,
            ChapterRepository chapterRepository,
            TextChunkRepository textChunkRepository,
            CourseService courseService,
            @Value("${ai4note.storage-root}") String storageRoot) {
        this.materialRepository = materialRepository;
        this.materialFileRepository = materialFileRepository;
        this.chapterRepository = chapterRepository;
        this.textChunkRepository = textChunkRepository;
        this.courseService = courseService;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public List<MaterialVO> list(Long courseId, Long userId) {
        courseService.getOwnedCourse(courseId, userId);
        return materialRepository.findByCourseIdOrderByUploadTimeDesc(courseId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    public MaterialVO detail(Long materialId, Long userId) {
        Material material = getOwnedMaterial(materialId, userId);
        return toVO(material);
    }

    @Transactional
    public MaterialVO upload(
            Long userId,
            Long courseId,
            Long chapterId,
            String title,
            String materialType,
            Integer year,
            Boolean key,
            String summary,
            MultipartFile file) {
        courseService.getOwnedCourse(courseId, userId);
        validateChapter(chapterId, courseId);
        validateFile(file);

        String originalName = cleanFileName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDirectory = storageRoot.resolve("uploads")
                .resolve("user-" + userId)
                .resolve("course-" + courseId);
        Path targetPath = targetDirectory.resolve(storedName).normalize();
        if (!targetPath.startsWith(targetDirectory)) {
            throw new BusinessException("文件路径不合法");
        }

        Material material = new Material();
        material.setCourseId(courseId);
        material.setChapterId(chapterId);
        material.setTitle(requireText(title, "资料标题不能为空"));
        material.setMaterialType(requireText(materialType, "资料类型不能为空").toUpperCase(Locale.ROOT));
        material.setYear(year);
        material.setKey(Boolean.TRUE.equals(key));
        material.setSummary(normalize(summary));

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            material = materialRepository.save(material);

            MaterialFile materialFile = new MaterialFile();
            materialFile.setMaterialId(material.getId());
            materialFile.setOriginalName(originalName);
            materialFile.setStoredName(storedName);
            materialFile.setFilePath(storageRoot.relativize(targetPath).toString().replace('\\', '/'));
            materialFile.setFileType(extension);
            materialFile.setFileSize(file.getSize());
            materialFile = materialFileRepository.save(materialFile);
            return MaterialVO.from(material, materialFile, 0);
        } catch (IOException exception) {
            LOGGER.error("Failed to save uploaded material to {}", targetPath, exception);
            deleteQuietly(targetPath);
            throw new BusinessException("保存资料文件失败");
        } catch (RuntimeException exception) {
            deleteQuietly(targetPath);
            throw exception;
        }
    }

    @Transactional
    public MaterialVO update(Long materialId, Long userId, MaterialUpdateRequest request) {
        Material material = getOwnedMaterial(materialId, userId);
        validateChapter(request.getChapterId(), material.getCourseId());
        material.setChapterId(request.getChapterId());
        material.setTitle(requireText(request.getTitle(), "资料标题不能为空"));
        material.setMaterialType(requireText(request.getMaterialType(), "资料类型不能为空")
                .toUpperCase(Locale.ROOT));
        material.setYear(request.getYear());
        material.setKey(request.getKey());
        material.setSummary(normalize(request.getSummary()));
        return toVO(materialRepository.save(material));
    }

    public Material getOwnedMaterial(Long materialId, Long userId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException("资料不存在"));
        courseService.getOwnedCourse(material.getCourseId(), userId);
        return material;
    }

    public Path resolveFilePath(Long materialId, Long userId) {
        getOwnedMaterial(materialId, userId);
        MaterialFile file = materialFileRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException("资料文件不存在"));
        Path path = storageRoot.resolve(file.getFilePath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            throw new BusinessException("资料文件不存在");
        }
        return path;
    }

    private MaterialVO toVO(Material material) {
        return MaterialVO.from(
                material,
                materialFileRepository.findByMaterialId(material.getId()).orElse(null),
                textChunkRepository.countByMaterialId(material.getId()));
    }

    private void validateChapter(Long chapterId, Long courseId) {
        if (chapterId == null) {
            return;
        }
        chapterRepository.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new BusinessException("章节不属于当前课程"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要上传的文件");
        }
        String extension = extensionOf(cleanFileName(file.getOriginalFilename()));
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 PDF、Word、Markdown 和 TXT 文件");
        }
    }

    private String cleanFileName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }
        return Paths.get(originalName).getFileName().toString();
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw new BusinessException("文件缺少有效扩展名");
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The database transaction still rolls back; orphan cleanup can be retried later.
        }
    }
}

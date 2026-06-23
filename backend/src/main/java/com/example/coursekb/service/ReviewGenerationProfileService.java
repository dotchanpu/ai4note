package com.example.coursekb.service;

import com.example.coursekb.dto.ReviewGenerationProfileRequest;
import com.example.coursekb.entity.ReviewGenerationProfile;
import com.example.coursekb.entity.TeacherProfile;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.ReviewGenerationProfileRepository;
import com.example.coursekb.mapper.TeacherProfileRepository;
import com.example.coursekb.vo.ReviewGenerationProfileVO;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewGenerationProfileService {
    private static final Set<String> DIFFICULTIES = new HashSet<>(Arrays.asList(
            "EASY", "MEDIUM", "MEDIUM_HARD", "HARD"));
    private static final Set<String> OUTPUT_TYPES = new HashSet<>(Arrays.asList(
            "REVIEW_NOTE", "OUTLINE", "FLASHCARDS", "MOCK_EXAM", "CHECKLIST"));

    private final CourseService courseService;
    private final ReviewGenerationProfileRepository reviewGenerationProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;

    public ReviewGenerationProfileService(
            CourseService courseService,
            ReviewGenerationProfileRepository reviewGenerationProfileRepository,
            TeacherProfileRepository teacherProfileRepository) {
        this.courseService = courseService;
        this.reviewGenerationProfileRepository = reviewGenerationProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
    }

    public List<ReviewGenerationProfileVO> list(Long userId, Long courseId) {
        courseService.getOwnedCourse(courseId, userId);
        return reviewGenerationProfileRepository.findByUserIdAndCourseIdOrderByUpdateTimeDescIdDesc(userId, courseId)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewGenerationProfileVO create(ReviewGenerationProfileRequest request) {
        courseService.getOwnedCourse(request.getCourseId(), request.getUserId());
        ReviewGenerationProfile profile = new ReviewGenerationProfile();
        apply(profile, request);
        return toVO(reviewGenerationProfileRepository.save(profile));
    }

    @Transactional
    public ReviewGenerationProfileVO update(Long profileId, ReviewGenerationProfileRequest request) {
        ReviewGenerationProfile profile = getOwnedProfile(profileId, request.getUserId());
        if (!profile.getCourseId().equals(request.getCourseId())) {
            throw new BusinessException("复习配置不属于当前课程");
        }
        courseService.getOwnedCourse(request.getCourseId(), request.getUserId());
        apply(profile, request);
        return toVO(reviewGenerationProfileRepository.save(profile));
    }

    @Transactional
    public void delete(Long profileId, Long userId) {
        ReviewGenerationProfile profile = getOwnedProfile(profileId, userId);
        courseService.getOwnedCourse(profile.getCourseId(), userId);
        reviewGenerationProfileRepository.delete(profile);
    }

    private ReviewGenerationProfile getOwnedProfile(Long profileId, Long userId) {
        return reviewGenerationProfileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new BusinessException("复习配置不存在或无权访问"));
    }

    private void apply(ReviewGenerationProfile profile, ReviewGenerationProfileRequest request) {
        profile.setUserId(request.getUserId());
        profile.setCourseId(request.getCourseId());
        profile.setTeacherProfileId(resolveTeacherProfileId(
                request.getTeacherProfileId(), request.getCourseId(), request.getUserId()));
        profile.setProfileName(requireText(request.getProfileName(), "配置名称不能为空"));
        profile.setTarget(normalizeText(request.getTarget()));
        profile.setDifficultyLevel(normalizeDifficulty(request.getDifficultyLevel()));
        profile.setOutputType(normalizeOutputType(request.getOutputType()));
        profile.setIncludePrerequisites(!Boolean.FALSE.equals(request.getIncludePrerequisites()));
        profile.setCustomRequirement(normalizeText(request.getCustomRequirement()));
    }

    private Long resolveTeacherProfileId(Long teacherProfileId, Long courseId, Long userId) {
        if (teacherProfileId == null) {
            return null;
        }
        TeacherProfile teacherProfile = teacherProfileRepository.findByIdAndUserId(teacherProfileId, userId)
                .orElseThrow(() -> new BusinessException("教师画像不存在或无权访问"));
        if (!courseId.equals(teacherProfile.getCourseId())) {
            throw new BusinessException("教师画像不属于当前课程");
        }
        return teacherProfile.getId();
    }

    private ReviewGenerationProfileVO toVO(ReviewGenerationProfile profile) {
        String teacherName = profile.getTeacherProfileId() == null
                ? null
                : teacherProfileRepository.findById(profile.getTeacherProfileId())
                        .map(TeacherProfile::getTeacherName)
                        .orElse(null);
        return ReviewGenerationProfileVO.from(profile, teacherName);
    }

    private String normalizeDifficulty(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "MEDIUM"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!DIFFICULTIES.contains(normalized)) {
            throw new BusinessException("不支持的复习难度：" + normalized);
        }
        return normalized;
    }

    private String normalizeOutputType(String value) {
        String normalized = value == null || value.trim().isEmpty()
                ? "REVIEW_NOTE"
                : value.trim().toUpperCase(Locale.ROOT);
        if (!OUTPUT_TYPES.contains(normalized)) {
            throw new BusinessException("不支持的输出类型：" + normalized);
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

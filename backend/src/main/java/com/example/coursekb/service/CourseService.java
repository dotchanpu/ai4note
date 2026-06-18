package com.example.coursekb.service;

import com.example.coursekb.dto.CourseRequest;
import com.example.coursekb.entity.Course;
import com.example.coursekb.exception.BusinessException;
import com.example.coursekb.mapper.CourseRepository;
import com.example.coursekb.mapper.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final UserAccountRepository userAccountRepository;

    public CourseService(
            CourseRepository courseRepository,
            UserAccountRepository userAccountRepository) {
        this.courseRepository = courseRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<Course> listByUser(Long userId) {
        ensureUserExists(userId);
        return courseRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public Course getOwnedCourse(Long courseId, Long userId) {
        return courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new BusinessException("课程不存在或无权访问"));
    }

    @Transactional
    public Course create(CourseRequest request) {
        ensureUserExists(request.getUserId());

        Course course = new Course();
        course.setUserId(request.getUserId());
        course.setCourseName(request.getCourseName().trim());
        course.setCourseCode(normalize(request.getCourseCode()));
        course.setSemester(normalize(request.getSemester()));
        course.setDescription(normalize(request.getDescription()));
        return courseRepository.save(course);
    }

    private void ensureUserExists(Long userId) {
        if (!userAccountRepository.existsById(userId)) {
            throw new BusinessException("用户不存在");
        }
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

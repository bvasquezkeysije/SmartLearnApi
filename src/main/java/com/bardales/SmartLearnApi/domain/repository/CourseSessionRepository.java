package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {
    List<CourseSession> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long courseId);

    Optional<CourseSession> findByIdAndCourseIdAndDeletedAtIsNull(Long id, Long courseId);

    Optional<CourseSession> findByIdAndCourseUserIdAndDeletedAtIsNull(Long id, Long userId);
}

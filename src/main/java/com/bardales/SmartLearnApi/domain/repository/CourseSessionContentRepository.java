package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseSessionContent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSessionContentRepository extends JpaRepository<CourseSessionContent, Long> {
    List<CourseSessionContent> findByCourseSessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long courseSessionId);

    List<CourseSessionContent> findByCourseWeekIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long courseWeekId);

    Optional<CourseSessionContent> findByIdAndCourseSessionIdAndDeletedAtIsNull(Long id, Long courseSessionId);
}

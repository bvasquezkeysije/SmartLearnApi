package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseCompetency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCompetencyRepository extends JpaRepository<CourseCompetency, Long> {
    List<CourseCompetency> findByCourseIdAndDeletedAtIsNullOrderBySortOrderAscCreatedAtAsc(Long courseId);
    Optional<CourseCompetency> findByIdAndCourseIdAndDeletedAtIsNull(Long id, Long courseId);
}


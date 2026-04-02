package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseWeekRepository extends JpaRepository<CourseWeek, Long> {
    List<CourseWeek> findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(Long courseSessionId);

    Optional<CourseWeek> findByIdAndCourseSessionIdAndDeletedAtIsNull(Long id, Long courseSessionId);
}

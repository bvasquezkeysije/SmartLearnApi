package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseWeekRepository extends JpaRepository<CourseWeek, Long> {
    List<CourseWeek> findByCourseSessionIdAndDeletedAtIsNullOrderByWeekOrderAscCreatedAtAsc(Long courseSessionId);

    List<CourseWeek> findByCourseSessionIdOrderByWeekOrderAscCreatedAtAsc(Long courseSessionId);

    @Query("select max(w.weekOrder) from CourseWeek w where w.courseSession.id = :courseSessionId")
    Integer findMaxWeekOrderByCourseSessionId(@Param("courseSessionId") Long courseSessionId);

    Optional<CourseWeek> findByIdAndCourseSessionIdAndDeletedAtIsNull(Long id, Long courseSessionId);
}

package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseSessionContent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseSessionContentRepository extends JpaRepository<CourseSessionContent, Long> {
    List<CourseSessionContent> findByCourseSessionIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(Long courseSessionId);

    List<CourseSessionContent> findByCourseSessionCourseIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(Long courseId);

    List<CourseSessionContent> findByCourseWeekIdAndDeletedAtIsNullOrderByContentOrderAscCreatedAtAsc(Long courseWeekId);

    Optional<CourseSessionContent> findByIdAndCourseSessionIdAndDeletedAtIsNull(Long id, Long courseSessionId);

    @Query("""
            SELECT COALESCE(MAX(c.contentOrder), 0)
            FROM CourseSessionContent c
            WHERE c.courseWeek.id = :courseWeekId AND c.deletedAt IS NULL
            """)
    Integer findMaxContentOrderByCourseWeekId(@Param("courseWeekId") Long courseWeekId);
}

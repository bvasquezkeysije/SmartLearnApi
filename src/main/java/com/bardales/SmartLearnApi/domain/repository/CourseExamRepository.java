package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseExam;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseExamRepository extends JpaRepository<CourseExam, Long> {
    List<CourseExam> findByCourseIdOrderByCreatedAtAsc(Long courseId);

    @Query("""
            select ce
            from CourseExam ce
            join fetch ce.exam e
            left join fetch e.user
            where ce.course.id in :courseIds
            order by ce.course.id asc, ce.createdAt asc
            """)
    List<CourseExam> findByCourseIdInOrderByCourseIdAscCreatedAtAsc(@Param("courseIds") List<Long> courseIds);

    void deleteByCourseId(Long courseId);
}

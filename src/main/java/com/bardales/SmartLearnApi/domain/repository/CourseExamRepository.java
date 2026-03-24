package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseExam;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseExamRepository extends JpaRepository<CourseExam, Long> {
    List<CourseExam> findByCourseIdOrderByCreatedAtAsc(Long courseId);
    void deleteByCourseId(Long courseId);
}

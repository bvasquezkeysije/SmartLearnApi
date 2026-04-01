package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    List<Course> findByVisibilityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(String visibility);
    Optional<Course> findByIdAndDeletedAtIsNull(Long id);
    Optional<Course> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
    boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);
    boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);
}

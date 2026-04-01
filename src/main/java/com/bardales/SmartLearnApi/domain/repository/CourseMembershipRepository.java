package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseMembershipRepository extends JpaRepository<CourseMembership, Long> {
    List<CourseMembership> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    List<CourseMembership> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long courseId);
    List<CourseMembership> findByCourseUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long ownerUserId);

    Optional<CourseMembership> findByCourseIdAndUserIdAndDeletedAtIsNull(Long courseId, Long userId);
    Optional<CourseMembership> findByCourseIdAndUserId(Long courseId, Long userId);
}

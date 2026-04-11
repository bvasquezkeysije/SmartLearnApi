package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.CourseMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseMembershipRepository extends JpaRepository<CourseMembership, Long> {
    List<CourseMembership> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    List<CourseMembership> findByCourseIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long courseId);

    @Query("""
            select m
            from CourseMembership m
            join fetch m.user
            where m.deletedAt is null
                and m.course.id in :courseIds
            order by m.course.id asc, m.createdAt asc
            """)
    List<CourseMembership> findByCourseIdInAndDeletedAtIsNullOrderByCourseIdAscCreatedAtAsc(@Param("courseIds") List<Long> courseIds);

    List<CourseMembership> findByCourseUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long ownerUserId);

    Optional<CourseMembership> findByCourseIdAndUserIdAndDeletedAtIsNull(Long courseId, Long userId);
    Optional<CourseMembership> findByCourseIdAndUserId(Long courseId, Long userId);
}

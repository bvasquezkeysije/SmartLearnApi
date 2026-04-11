package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamMembershipRepository extends JpaRepository<ExamMembership, Long> {
    List<ExamMembership> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    List<ExamMembership> findByExamIdInAndUserIdAndDeletedAtIsNull(List<Long> examIds, Long userId);
    Optional<ExamMembership> findByExamIdAndUserIdAndDeletedAtIsNull(Long examId, Long userId);
    Optional<ExamMembership> findTopByExamIdAndUserIdOrderByIdDesc(Long examId, Long userId);
    List<ExamMembership> findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long examId);
    long countByExamIdAndDeletedAtIsNull(Long examId);

    @Query("""
            select m.exam.id, count(m)
            from ExamMembership m
            where m.exam.id in :examIds and m.deletedAt is null
            group by m.exam.id
            """)
    List<Object[]> countByExamIdsGrouped(@Param("examIds") List<Long> examIds);
}

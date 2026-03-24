package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamMembershipRepository extends JpaRepository<ExamMembership, Long> {
    List<ExamMembership> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);
    Optional<ExamMembership> findByExamIdAndUserIdAndDeletedAtIsNull(Long examId, Long userId);
    List<ExamMembership> findByExamIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long examId);
    long countByExamIdAndDeletedAtIsNull(Long examId);
}

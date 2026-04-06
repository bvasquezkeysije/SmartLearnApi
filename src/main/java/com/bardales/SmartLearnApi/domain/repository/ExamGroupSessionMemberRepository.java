package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamGroupSessionMemberRepository extends JpaRepository<ExamGroupSessionMember, Long> {
    Optional<ExamGroupSessionMember> findBySessionIdAndUserIdAndDeletedAtIsNull(Long sessionId, Long userId);
    Optional<ExamGroupSessionMember> findTopBySessionIdAndUserIdOrderByIdDesc(Long sessionId, Long userId);

    List<ExamGroupSessionMember> findBySessionIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long sessionId);

    long countBySessionIdAndDeletedAtIsNull(Long sessionId);
}

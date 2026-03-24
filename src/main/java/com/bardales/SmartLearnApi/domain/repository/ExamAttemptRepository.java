package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    long countByExamIdAndUserId(Long examId, Long userId);
    List<ExamAttempt> findByExamIdInAndUserIdIn(List<Long> examIds, List<Long> userIds);
}

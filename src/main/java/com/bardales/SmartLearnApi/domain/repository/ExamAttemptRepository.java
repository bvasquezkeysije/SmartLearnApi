package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    long countByExamIdAndUserId(Long examId, Long userId);
    List<ExamAttempt> findByExamIdInAndUserIdIn(List<Long> examIds, List<Long> userIds);

    @Query("""
            select a.exam.id, count(a)
            from ExamAttempt a
            where a.exam.id in :examIds and a.user.id = :userId
            group by a.exam.id
            """)
    List<Object[]> countByExamIdsAndUserIdGrouped(@Param("examIds") List<Long> examIds, @Param("userId") Long userId);
}

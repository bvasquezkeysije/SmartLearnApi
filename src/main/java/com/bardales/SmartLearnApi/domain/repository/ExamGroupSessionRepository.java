package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamGroupSession;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamGroupSessionRepository extends JpaRepository<ExamGroupSession, Long> {
    Optional<ExamGroupSession> findByIdAndExamIdAndDeletedAtIsNull(Long id, Long examId);

    Optional<ExamGroupSession> findTopByExamIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDesc(
            Long examId, Collection<String> statuses);

        List<ExamGroupSession> findByExamIdInAndDeletedAtIsNullAndStatusInOrderByExamIdAscCreatedAtDesc(
            List<Long> examIds,
            Collection<String> statuses);

        @Query(
                        """
                        select count(s)
                        from ExamGroupSession s
                        where s.exam.id = :examId
                            and s.deletedAt is null
                            and (
                                        s.startedAt is not null
                                        or s.finishedAt is not null
                                        or lower(coalesce(s.status, '')) = 'finished'
                                    )
                        """)
        long countPracticedByExamId(@Param("examId") Long examId);

        @Query(
                        """
                        select s.exam.id, count(s)
                        from ExamGroupSession s
                        where s.exam.id in :examIds
                            and s.deletedAt is null
                            and (
                                        s.startedAt is not null
                                        or s.finishedAt is not null
                                        or lower(coalesce(s.status, '')) = 'finished'
                                    )
                        group by s.exam.id
                        """)
        List<Object[]> countPracticedByExamIdsGrouped(@Param("examIds") List<Long> examIds);
}

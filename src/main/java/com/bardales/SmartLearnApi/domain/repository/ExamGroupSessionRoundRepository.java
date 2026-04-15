package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionRound;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamGroupSessionRoundRepository extends JpaRepository<ExamGroupSessionRound, Long> {
    Optional<ExamGroupSessionRound> findBySession_IdAndRoundNumberAndDeletedAtIsNull(Long sessionId, Integer roundNumber);
}


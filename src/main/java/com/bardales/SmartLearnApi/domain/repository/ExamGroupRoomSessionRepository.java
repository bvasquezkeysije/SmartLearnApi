package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamGroupRoomSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamGroupRoomSessionRepository extends JpaRepository<ExamGroupRoomSession, Long> {
    Optional<ExamGroupRoomSession> findTopBySessionIdAndUserIdAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(
            Long sessionId, Long userId);

    Optional<ExamGroupRoomSession> findTopBySessionIdAndRoomTokenAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(
            Long sessionId, String roomToken);
}


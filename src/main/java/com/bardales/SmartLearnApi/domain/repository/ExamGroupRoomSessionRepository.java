package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamGroupRoomSession;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamGroupRoomSessionRepository extends JpaRepository<ExamGroupRoomSession, Long> {
    Optional<ExamGroupRoomSession> findTopBySessionIdAndUserIdAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(
            Long sessionId, Long userId);

    Optional<ExamGroupRoomSession> findTopBySessionIdAndRoomTokenAndDeletedAtIsNullAndRevokedAtIsNullOrderByIdDesc(
            Long sessionId, String roomToken);

    @Modifying
    @Query("""
        UPDATE ExamGroupRoomSession rs
        SET rs.revokedAt = :revokedAt
        WHERE rs.session.id = :sessionId
          AND rs.deletedAt IS NULL
          AND rs.revokedAt IS NULL
        """)
    int revokeActiveBySessionId(@Param("sessionId") Long sessionId, @Param("revokedAt") LocalDateTime revokedAt);
}

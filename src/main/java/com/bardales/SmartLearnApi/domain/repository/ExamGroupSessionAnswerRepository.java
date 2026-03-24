package com.bardales.SmartLearnApi.domain.repository;

import com.bardales.SmartLearnApi.domain.entity.ExamGroupSessionAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamGroupSessionAnswerRepository extends JpaRepository<ExamGroupSessionAnswer, Long> {
    @Query("""
        SELECT answer
        FROM ExamGroupSessionAnswer answer
        WHERE answer.session.id = :sessionId
          AND answer.user.id = :userId
          AND answer.question.id = :questionId
        """)
    Optional<ExamGroupSessionAnswer> findForUserQuestion(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId,
        @Param("questionId") Long questionId);

    @Query("""
        SELECT answer
        FROM ExamGroupSessionAnswer answer
        WHERE answer.session.id = :sessionId
          AND answer.user.id = :userId
          AND answer.question.id = :questionId
        ORDER BY answer.answeredAt DESC, answer.createdAt DESC, answer.id DESC
        """)
    List<ExamGroupSessionAnswer> findAllForUserQuestion(
        @Param("sessionId") Long sessionId,
        @Param("userId") Long userId,
        @Param("questionId") Long questionId);

    @Query("""
        SELECT answer
        FROM ExamGroupSessionAnswer answer
        WHERE answer.session.id = :sessionId
          AND answer.question.id = :questionId
        ORDER BY answer.answeredAt ASC, answer.id ASC
        """)
    List<ExamGroupSessionAnswer> findForQuestion(
        @Param("sessionId") Long sessionId,
        @Param("questionId") Long questionId);

    @Modifying
    @Query("""
        DELETE FROM ExamGroupSessionAnswer answer
        WHERE answer.id IN :ids
        """)
    void deleteAllByIds(@Param("ids") List<Long> ids);

    void deleteBySession_Id(Long sessionId);
}

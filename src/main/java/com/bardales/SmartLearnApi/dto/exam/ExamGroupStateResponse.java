package com.bardales.SmartLearnApi.dto.exam;

import java.time.LocalDateTime;
import java.util.List;

public record ExamGroupStateResponse(
        Long sessionId,
        Long examId,
        String examName,
        String status,
        Integer totalQuestions,
        Integer currentQuestionIndex,
        Boolean allAnsweredCurrent,
        Boolean canStartGroup,
        QuestionResponse currentQuestion,
        List<ExamGroupCurrentAnswerResponse> currentAnswers,
        List<ExamGroupParticipantStateResponse> participants,
        List<ExamGroupRankingEntryResponse> finalRanking,
        String firstResponderName,
        Integer firstAnswerElapsedSeconds,
        LocalDateTime questionStartedAt,
        Long questionStartedAtEpochMs,
        String phase,
        LocalDateTime phaseStartedAt,
        LocalDateTime phaseEndsAt,
        Long phaseStartedAtEpochMs,
        Long phaseEndsAtEpochMs,
        Integer questionVersion,
        Boolean revealAnswers,
        Boolean reviewActive,
        Integer reviewSecondsRemaining,
        Long serverNowEpochMs,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {}

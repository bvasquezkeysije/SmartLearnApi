package com.bardales.SmartLearnApi.dto.exam;

import java.time.LocalDateTime;

public record ExamSummaryResponse(
        Long id,
        String name,
        String code,
        String sourceFilePath,
        Integer questionsCount,
        Long personalPracticeCount,
        Long groupPracticeCount,
        Long attemptsCount,
        Boolean practiceFeedbackEnabled,
        String practiceOrderMode,
        Boolean practiceRepeatUntilCorrect,
        Long ownerUserId,
        String visibility,
        String accessRole,
        Boolean canEditQuestions,
        Boolean canEditSettings,
        Boolean canShare,
        Boolean canStartGroup,
        Boolean canRenameExam,
        Long participantsCount,
        Long groupPracticeSessionId,
        String groupPracticeStatus,
        Long groupPracticeCreatedByUserId,
        LocalDateTime createdAt) {
}

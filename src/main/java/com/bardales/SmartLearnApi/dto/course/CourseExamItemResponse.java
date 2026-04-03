package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;

public record CourseExamItemResponse(
        Long id,
        String name,
        Integer questionsCount,
        String code,
        Long ownerUserId,
        String visibility,
        String accessRole,
        Boolean canEditQuestions,
        Boolean canEditSettings,
        Boolean canShare,
        Boolean canStartGroup,
        Boolean canRenameExam,
        Long participantsCount,
        Long personalPracticeCount,
        Long groupPracticeCount,
        Long attemptsCount,
        Long groupPracticeSessionId,
        String groupPracticeStatus,
        Long groupPracticeCreatedByUserId,
        LocalDateTime createdAt) {
}

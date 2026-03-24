package com.bardales.SmartLearnApi.dto.exam;

import java.time.LocalDateTime;

public record ExamPracticeStartResponse(
        Long attemptId,
        Integer totalQuestions,
        String orderMode,
        Boolean feedbackEnabled,
        Boolean repeatUntilCorrect,
        LocalDateTime startedAt) {
}

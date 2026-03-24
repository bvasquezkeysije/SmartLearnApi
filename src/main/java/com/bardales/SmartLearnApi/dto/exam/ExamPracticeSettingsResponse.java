package com.bardales.SmartLearnApi.dto.exam;

public record ExamPracticeSettingsResponse(
        String practiceFeedbackMode,
        String practiceOrderMode,
        String practiceProgressMode) {
}

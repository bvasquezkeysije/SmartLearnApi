package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamIndividualPracticeSettingsRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "practiceFeedbackMode es obligatorio") String practiceFeedbackMode,
        @NotBlank(message = "practiceOrderMode es obligatorio") String practiceOrderMode,
        @NotBlank(message = "practiceProgressMode es obligatorio") String practiceProgressMode) {
}

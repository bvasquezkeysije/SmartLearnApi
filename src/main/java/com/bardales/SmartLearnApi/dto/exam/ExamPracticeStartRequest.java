package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotNull;

public record ExamPracticeStartRequest(@NotNull(message = "userId es obligatorio") Long userId) {
}

package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotNull;

public record ExamGroupStartRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotNull(message = "sessionId es obligatorio") Long sessionId) {}

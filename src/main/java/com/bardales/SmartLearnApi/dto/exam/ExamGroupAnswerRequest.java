package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotNull;

public record ExamGroupAnswerRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotNull(message = "sessionId es obligatorio") Long sessionId,
        @NotNull(message = "questionId es obligatorio") Long questionId,
        String selectedOption,
        String writtenAnswer) {}

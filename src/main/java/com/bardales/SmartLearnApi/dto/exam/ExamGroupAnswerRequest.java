package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotNull;

public record ExamGroupAnswerRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotNull(message = "sessionId es obligatorio") Long sessionId,
        @NotNull(message = "questionId es obligatorio") Long questionId,
        @NotNull(message = "questionVersion es obligatorio") Integer questionVersion,
        String selectedOption,
        String writtenAnswer,
        String roomSessionToken) {}

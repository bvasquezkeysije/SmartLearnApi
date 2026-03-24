package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManualQuestionUpsertRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "questionText es obligatorio") String questionText,
        @NotBlank(message = "questionType es obligatorio") String questionType,
        String correctAnswer,
        String explanation,
        @NotNull(message = "points es obligatorio") @Min(value = 1) @Max(value = 1000) Integer points,
        @NotNull(message = "temporizadorSegundos es obligatorio") @Min(value = 1) @Max(value = 86400) Integer temporizadorSegundos,
        @Min(value = 1) @Max(value = 3600) Integer reviewSeconds,
        Boolean timerEnabled,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String correctOption) {
}
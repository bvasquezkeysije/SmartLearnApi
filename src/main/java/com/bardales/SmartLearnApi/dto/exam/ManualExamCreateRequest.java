package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManualExamCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "manualExamName es obligatorio") String manualExamName) {
}
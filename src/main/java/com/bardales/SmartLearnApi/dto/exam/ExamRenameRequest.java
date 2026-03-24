package com.bardales.SmartLearnApi.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamRenameRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "examName es obligatorio") String examName) {
}
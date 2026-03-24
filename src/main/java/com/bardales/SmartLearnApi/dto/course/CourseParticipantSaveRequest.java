package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseParticipantSaveRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "identifier es obligatorio") String identifier,
        String role) {
}


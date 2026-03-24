package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotNull;

public record CourseParticipantRoleUpdateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        String role) {
}


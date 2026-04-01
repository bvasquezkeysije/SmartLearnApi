package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotNull;

public record CourseJoinRequest(
        @NotNull(message = "userId es obligatorio") Long userId) {
}

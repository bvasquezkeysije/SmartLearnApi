package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotNull;

public record CourseWeekSaveRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        String name,
        String description,
        Integer weekOrder) {}

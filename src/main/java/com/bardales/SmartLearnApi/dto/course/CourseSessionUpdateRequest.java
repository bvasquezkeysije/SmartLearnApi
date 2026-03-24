package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseSessionUpdateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "name es obligatorio") String name,
        String weeklyContent) {}

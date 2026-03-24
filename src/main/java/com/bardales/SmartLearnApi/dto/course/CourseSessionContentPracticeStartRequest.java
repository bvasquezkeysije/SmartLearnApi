package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotNull;

public record CourseSessionContentPracticeStartRequest(@NotNull(message = "userId es obligatorio") Long userId) {}

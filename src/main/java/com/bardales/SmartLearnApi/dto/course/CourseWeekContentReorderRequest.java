package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CourseWeekContentReorderRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotEmpty(message = "orderedContentIds es obligatorio") List<Long> orderedContentIds) {}

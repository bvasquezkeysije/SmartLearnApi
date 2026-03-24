package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CourseSetExamsRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        List<Long> examIds) {
}

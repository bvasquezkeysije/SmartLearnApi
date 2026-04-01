package com.bardales.SmartLearnApi.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "name es obligatorio") String name,
        String description,
        String coverImageData,
        String code,
        String visibility,
        String joinMode,
        String priority,
        Integer sortOrder) {
}

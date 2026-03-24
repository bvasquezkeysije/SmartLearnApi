package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;

public record CourseCompetencyItemResponse(
        Long id,
        String name,
        String description,
        String level,
        Integer sortOrder,
        LocalDateTime createdAt) {
}


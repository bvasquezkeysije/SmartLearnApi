package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;
import java.util.List;

public record CourseSessionItemResponse(
        Long id,
        String name,
        String weeklyContent,
        List<CourseSessionContentItemResponse> contents,
        LocalDateTime createdAt) {
}

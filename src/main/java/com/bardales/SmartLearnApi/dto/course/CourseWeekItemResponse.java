package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;
import java.util.List;

public record CourseWeekItemResponse(
        Long id,
        Integer weekOrder,
        String name,
        String description,
        List<CourseSessionContentItemResponse> contents,
        LocalDateTime createdAt) {}

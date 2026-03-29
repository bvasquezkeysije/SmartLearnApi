package com.bardales.SmartLearnApi.dto.schedule;

import java.time.LocalDateTime;

public record ScheduleActivityResponse(
        Long id,
        String title,
        String description,
        String day,
        String startTime,
        String endTime,
        String location,
        String color,
        Integer sortOrder,
        LocalDateTime createdAt) {}
package com.bardales.SmartLearnApi.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScheduleActivitySaveRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "title es obligatorio") String title,
        String description,
        @NotBlank(message = "day es obligatorio") String day,
        @NotBlank(message = "startTime es obligatorio") String startTime,
        @NotBlank(message = "endTime es obligatorio") String endTime,
        String location,
        String color,
        Integer sortOrder) {}

package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;

public record CourseGradeItemResponse(
        Long userId,
        String name,
        String username,
        String email,
        Integer attemptsCount,
        Double averageScore,
        Double bestScore,
        Double lastScore,
        LocalDateTime lastAttemptAt) {
}


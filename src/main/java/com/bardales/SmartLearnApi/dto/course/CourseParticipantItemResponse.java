package com.bardales.SmartLearnApi.dto.course;

import java.time.LocalDateTime;

public record CourseParticipantItemResponse(
        Long id,
        Long userId,
        String name,
        String username,
        String email,
        String role,
        Boolean owner,
        LocalDateTime joinedAt) {
}


package com.bardales.SmartLearnApi.dto.exam;

import java.time.LocalDateTime;

public record ExamParticipantResponse(
        Long userId,
        String name,
        String username,
        String email,
        String profileImageUrl,
        String role,
        Boolean canShare,
        Boolean canStartGroup,
        Boolean canRenameExam,
        Boolean owner,
        LocalDateTime joinedAt) {}

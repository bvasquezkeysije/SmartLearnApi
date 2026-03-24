package com.bardales.SmartLearnApi.dto.exam;

public record ExamGroupParticipantStateResponse(
        Long userId,
        String name,
        String username,
        String profileImageUrl,
        String role,
        Boolean canStartGroup,
        Boolean owner,
        Boolean connected,
        Boolean answeredCurrent,
        Boolean correctCurrent) {}

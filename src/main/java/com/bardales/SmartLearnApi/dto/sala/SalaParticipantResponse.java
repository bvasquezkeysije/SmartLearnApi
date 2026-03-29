package com.bardales.SmartLearnApi.dto.sala;

public record SalaParticipantResponse(
        Long id,
        Long userId,
        String name,
        Boolean micOn,
        Boolean isScreenSharing) {}

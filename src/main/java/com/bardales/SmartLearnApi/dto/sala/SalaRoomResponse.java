package com.bardales.SmartLearnApi.dto.sala;

import java.time.LocalDateTime;
import java.util.List;

public record SalaRoomResponse(
        Long id,
        String name,
        String code,
        String visibility,
        String description,
        String imageData,
        Long ownerUserId,
        String accessRole,
        Boolean canEdit,
        Boolean canShare,
        List<SalaParticipantResponse> participants,
        List<SalaMessageResponse> messages,
        LocalDateTime createdAt) {}

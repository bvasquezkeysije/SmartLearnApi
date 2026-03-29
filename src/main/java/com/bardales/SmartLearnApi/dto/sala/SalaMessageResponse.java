package com.bardales.SmartLearnApi.dto.sala;

import java.time.LocalDateTime;

public record SalaMessageResponse(
        Long id,
        Long senderUserId,
        String sender,
        String content,
        Boolean isCurrentUser,
        LocalDateTime createdAt) {}

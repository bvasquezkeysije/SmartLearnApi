package com.bardales.SmartLearnApi.dto.chat;

import java.time.LocalDateTime;

public record ChatSummaryResponse(
        Long id,
        String name,
        long messagesCount,
        LocalDateTime createdAt) {
}
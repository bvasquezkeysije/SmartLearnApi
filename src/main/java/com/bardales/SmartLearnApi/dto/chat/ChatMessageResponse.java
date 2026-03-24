package com.bardales.SmartLearnApi.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String role,
        String content,
        LocalDateTime createdAt) {
}
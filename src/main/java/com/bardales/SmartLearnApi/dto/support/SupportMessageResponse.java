package com.bardales.SmartLearnApi.dto.support;

import java.time.LocalDateTime;

public record SupportMessageResponse(
        Long id,
        Long conversationId,
        Long senderUserId,
        String senderName,
        String senderUsername,
        String senderRole,
        String content,
        LocalDateTime createdAt) {
}


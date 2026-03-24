package com.bardales.SmartLearnApi.dto.share;

import java.time.LocalDateTime;

public record ShareNotificationResponse(
        Long id,
        Long senderUserId,
        String senderName,
        String senderUsername,
        String resourceType,
        Long resourceId,
        String resourceName,
        String message,
        String token,
        LocalDateTime readAt,
        LocalDateTime createdAt) {}


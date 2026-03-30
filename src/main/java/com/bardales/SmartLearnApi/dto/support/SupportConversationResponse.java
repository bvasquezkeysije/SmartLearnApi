package com.bardales.SmartLearnApi.dto.support;

import java.time.LocalDateTime;

public record SupportConversationResponse(
        Long id,
        Long requesterUserId,
        String requesterName,
        String requesterUsername,
        Long assignedAdminUserId,
        String assignedAdminName,
        String subject,
        String status,
        String priority,
        String channelPreference,
        String ticketType,
        String moduleKey,
        String whatsappNumber,
        String callNumber,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt) {
}

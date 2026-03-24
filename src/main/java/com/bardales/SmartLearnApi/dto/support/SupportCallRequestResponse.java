package com.bardales.SmartLearnApi.dto.support;

import java.time.LocalDateTime;

public record SupportCallRequestResponse(
        Long id,
        Long requesterUserId,
        String requesterName,
        String requesterUsername,
        String phoneNumber,
        String preferredSchedule,
        String reason,
        String status,
        Long handledByAdminUserId,
        LocalDateTime handledAt,
        LocalDateTime createdAt) {
}


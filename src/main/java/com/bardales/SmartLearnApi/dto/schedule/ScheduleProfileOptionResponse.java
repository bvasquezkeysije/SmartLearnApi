package com.bardales.SmartLearnApi.dto.schedule;

import java.time.LocalDateTime;

public record ScheduleProfileOptionResponse(
        Long profileId,
        String profileName,
        Long ownerUserId,
        String ownerName,
        String accessRole,
        Boolean canEdit,
        Boolean canShare,
        LocalDateTime createdAt) {}

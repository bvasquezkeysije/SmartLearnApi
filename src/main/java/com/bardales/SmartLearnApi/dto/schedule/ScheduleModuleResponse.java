package com.bardales.SmartLearnApi.dto.schedule;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleModuleResponse(
        Long profileId,
        String profileName,
        String description,
        Long ownerUserId,
        String accessRole,
        Boolean canEdit,
        Boolean canShare,
        List<ScheduleProfileOptionResponse> profiles,
        String referenceImageData,
        String referenceImageName,
        List<ScheduleActivityResponse> activities,
        LocalDateTime createdAt) {}

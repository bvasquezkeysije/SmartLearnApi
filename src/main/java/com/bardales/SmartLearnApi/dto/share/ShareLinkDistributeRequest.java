package com.bardales.SmartLearnApi.dto.share;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ShareLinkDistributeRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "resourceType es obligatorio") String resourceType,
        @NotNull(message = "resourceId es obligatorio") Long resourceId,
        String resourceName,
        List<Long> recipientUserIds,
        Integer expiresInHours,
        String examVisibility,
        String examRole,
        Boolean examCanShare) {}

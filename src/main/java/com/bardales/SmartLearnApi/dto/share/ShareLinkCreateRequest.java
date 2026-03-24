package com.bardales.SmartLearnApi.dto.share;

import jakarta.validation.constraints.NotNull;

public record ShareLinkCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        Integer expiresInHours) {}

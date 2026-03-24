package com.bardales.SmartLearnApi.dto.share;

import java.time.LocalDateTime;

public record ShareLinkResponse(
        Long id,
        String resourceType,
        Long resourceId,
        String token,
        LocalDateTime expiresAt,
        Integer claimsCount) {}

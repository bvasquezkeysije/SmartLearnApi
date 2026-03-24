package com.bardales.SmartLearnApi.dto.share;

import java.time.LocalDateTime;

public record ShareLinkDistributeResponse(
        Long shareLinkId,
        String resourceType,
        Long resourceId,
        String token,
        LocalDateTime expiresAt,
        Integer notificationsCreated) {}


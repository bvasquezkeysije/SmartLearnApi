package com.bardales.SmartLearnApi.dto.share;

public record ShareLinkClaimResponse(
        String resourceType,
        Long resourceId,
        String resourceName,
        String message) {}

package com.bardales.SmartLearnApi.dto.mobile;

import java.time.LocalDateTime;

public record AndroidReleaseResponse(
        Long id,
        String versionName,
        Integer versionCode,
        String apkUrl,
        String checksumSha256,
        String releaseNotes,
        Boolean isActive,
        Long createdByUserId,
        LocalDateTime createdAt) {
}

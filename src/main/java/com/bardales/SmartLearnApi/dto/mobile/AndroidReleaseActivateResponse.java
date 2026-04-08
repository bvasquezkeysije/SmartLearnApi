package com.bardales.SmartLearnApi.dto.mobile;

public record AndroidReleaseActivateResponse(
        Long id,
        String versionName,
        Integer versionCode,
        Boolean isActive,
        String message) {
}

package com.bardales.SmartLearnApi.dto.auth;

public record ProfileImageUpdateRequest(
        String profileImageData,
        Double profileImageScale,
        Double profileImageOffsetX,
        Double profileImageOffsetY) {
}

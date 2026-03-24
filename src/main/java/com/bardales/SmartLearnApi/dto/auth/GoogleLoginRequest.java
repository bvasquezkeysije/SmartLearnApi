package com.bardales.SmartLearnApi.dto.auth;

public record GoogleLoginRequest(
        String idToken,
        String accessToken) {
}

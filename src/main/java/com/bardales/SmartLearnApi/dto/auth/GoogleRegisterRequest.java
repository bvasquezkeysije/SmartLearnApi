package com.bardales.SmartLearnApi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleRegisterRequest(
        String idToken,
        String accessToken,
        @NotBlank(message = "username es obligatorio") String username,
        String name) {
}

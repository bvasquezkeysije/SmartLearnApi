package com.bardales.SmartLearnApi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "identifier es obligatorio") String identifier,
        @NotBlank(message = "password es obligatorio") String password) {
}
package com.bardales.SmartLearnApi.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupportMessageCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "message es obligatorio") String message) {
}


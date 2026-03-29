package com.bardales.SmartLearnApi.dto.sala;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalaMessageCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "content es obligatorio") String content) {}

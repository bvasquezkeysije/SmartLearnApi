package com.bardales.SmartLearnApi.dto.sala;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalaCreateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "name es obligatorio") String name,
        @NotBlank(message = "code es obligatorio") String code,
        @NotBlank(message = "visibility es obligatorio") String visibility,
        String description,
        String imageData) {}

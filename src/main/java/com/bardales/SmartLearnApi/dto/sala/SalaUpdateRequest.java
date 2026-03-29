package com.bardales.SmartLearnApi.dto.sala;

import jakarta.validation.constraints.NotNull;

public record SalaUpdateRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        String name,
        String code,
        String visibility,
        String description,
        String imageData) {}

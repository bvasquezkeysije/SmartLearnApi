package com.bardales.SmartLearnApi.dto.sala;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalaJoinByCodeRequest(
        @NotNull(message = "userId es obligatorio") Long userId,
        @NotBlank(message = "code es obligatorio") String code) {}
